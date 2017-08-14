package metascala
import scala.collection.mutable
import imm.Type.Prim._
import metascala.imm.Type.Prim
import metascala.natives.Bindings
import metascala.util.{Constants, Ref, Util}

import scala.reflect.ClassTag

object Virtualizer {
  def toRealObj[T](v: Int)(implicit vm: Bindings.Interface, ct: ClassTag[T]) = {
    Virtualizer.popVirtual(ct.runtimeClass.getName.replace('.', '/'), () => v)
      .asInstanceOf[T]
  }
  def toVirtObj(x: Any)(implicit registrar: rt.Allocator) = {
    Virtualizer.pushVirtual(x).apply(0)
  }

  lazy val unsafe = {
    val field = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe")
    field.setAccessible(true)
    val f = field.get(null)
    val g = f.asInstanceOf[sun.misc.Unsafe]
    g
  }

  def popVirtual(tpe: imm.Type,
                 src: () => Int,
                 refs: mutable.Map[Int, Any] = mutable.Map.empty)
                (implicit vm: Bindings.Interface): Any = {
    val x = tpe match {
      case V => ()
      case p: imm.Type.Prim[_] => p.read(src)
      case _ => //reference type
        val address = src()
        if(address == 0) null
        else if (refs.contains(address)) refs(address)
        else tpe match{
          case imm.Type.Cls("java/lang/Object") | imm.Type.Arr(_) if vm.isArr(address) =>

            val tpe = vm.arr(address).innerType

            val clsObj = tpe match{
              case v: imm.Type.Prim[_] => v.primClass
              case _ => Class.forName(tpe.name.replace('/', '.'))
            }

            val newArr = java.lang.reflect.Array.newInstance(clsObj, vm.arr(address).arrayLength)

            for(i <- 0 until vm.arr(address).arrayLength){

              val cooked = tpe match{
                case p: imm.Type.Prim[_] => p.read(Util.reader(vm.heap.memory, address + Constants.arrayHeaderSize + i * tpe.size))
                case x => popVirtual(tpe, Util.reader(vm.heap.memory, address + Constants.arrayHeaderSize + i * tpe.size))
              }
              java.lang.reflect.Array.set(newArr, i, cooked)
            }

            newArr
          case t @ name=>
            val obj = unsafe.allocateInstance(Class.forName(vm.obj(address).cls.name.replace('/', '.')))
            refs += (address -> obj)
            var index = 0
            for(field <- vm.obj(address).cls.fieldList.distinct){
              // workaround for http://bugs.sun.com/view_bug.do?bug_id=4763881
              if (field.name == "backtrace") index += 1 // just skip it
              else{
                val f = getAllFields(obj.getClass).find(_.getName == field.name).get
                f.setAccessible(true)
                val popped = popVirtual(field.desc, Util.reader(vm.heap.memory, address + Constants.objectHeaderSize + index), refs)
                f.set(obj, popped )
                index += field.desc.size
              }
            }
            obj

        }
    }

    x
  }

  def getAllFields(cls: Class[_]): Seq[java.lang.reflect.Field] = {
    Option(cls.getSuperclass)
      .toSeq
      .flatMap(getAllFields)
      .++(cls.getDeclaredFields)
  }

  def pushVirtual(thing: Any)(implicit registrar: rt.Allocator): Seq[Int] = {
    val tmp = new mutable.Stack[Int]()
    pushVirtual(thing, tmp.push(_))
    tmp.reverse
  }

  def pushVirtual(thing: Any, out: Int => Unit)(implicit registrar: rt.Allocator): Unit = {

    implicit val vm = registrar.vm
    thing match {
      case null => out(0)
      case b: Boolean => Z.write(b, out)
      case b: Byte    => B.write(b, out)
      case b: Char    => C.write(b, out)
      case b: Short   => S.write(b, out)
      case b: Int     => I.write(b, out)
      case b: Float   => F.write(b, out)
      case b: Long    => J.write(b, out)
      case b: Double  => D.write(b, out)
      case b: Array[_] =>

        val tpe = imm.Type.Arr.read(b.getClass.getName.replace('.', '/')).innerType
        val arr =
          registrar.newArr(
            tpe,
            b.flatMap(pushVirtual).map{x =>
              val ref : Ref = new Ref.ManualRef(x)
              if (!b.getClass.getComponentType.isPrimitive) {
                registrar.register(ref)
              }
              ref
            }
          )

        out(arr.address())
      case b: Any =>
        var index = 0
        val contents = mutable.Buffer.empty[Ref]
        val decFields = b.getClass.getDeclaredFields
        for(field <- vm.ClsTable(imm.Type.Cls.apply(b.getClass.getName)).fieldList.distinct) yield {
          val f = decFields.find(_.getName == field.name).get
          f.setAccessible(true)
          pushVirtual(f.get(b), x => {
            contents.append(x)
            if (!f.getType.isPrimitive) {
              registrar.register(contents.last)
            }
          })
          index += field.desc.size
        }

        val obj = registrar.newObj(vm.ClsTable(b.getClass.getName))
        contents.map(_()).map(Util.writer(vm.heap.memory, obj.address() + Constants.objectHeaderSize))
        out(obj.address())
    }
  }
}
