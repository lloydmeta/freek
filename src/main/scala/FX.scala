package freek

/** a type helper to build Coproduct of effects F[_] with a clean syntax
  *
  * FXNil is equivalent to higher-kinded CNil
  * to build the equivalent of [t => F[t] :+: G[t] :+: CNilk[t]], use following syntax
  *
  * ```
  * type F[_]
  * type G[_]
  * type H[_]
  *
  * type C[t] = (G :|: H :|: FXNil)#Cop[t] (gives [t => G[t] :+: H[t] :+: CNilK[t])
  *
  * type C2[t] = (F :||: C)#Cop[t] (gives [t => F[t] :+: G[t] :+: H[t] :+: CNilK[t])
  * ```
  */
sealed trait FX {
  type Cop[_] <: CoproductK[_]
}

trait :|:[H[_], T <: FX] extends FX {
  // type Cop[t] = ConsK[H, T#Cop, t]
  type Cop[t] = AppendK[In1[H, ?], T#Cop, t]
}

// Relying on AppendK that delays type unification later
trait :||:[T1 <: FX, T2 <: FX] extends FX {
  type Cop[t] = AppendK[T1#Cop, T2#Cop, t]
}

trait FXNil extends FX {
  type Cop[t] = CNilK[t]
}

trait ToCopK[F <: FX] {
  type Cop[_] <: CoproductK[_]
}

object ToCopK extends LowerToCopK {

  def apply[F <: FX](implicit toCopK: ToCopK[F]) = toCopK

  type Aux[F <: FX, C[_] <: CoproductK[_]] = ToCopK[F] {
    type Cop[t] = C[t]
  }

  implicit val fxnil: ToCopK.Aux[FXNil, CNilK] = new ToCopK[FXNil] {
    type Cop[t] = CNilK[t]
  }

  implicit def one[H[_]]: ToCopK.Aux[:|:[H, FXNil], In1[H, ?]] =
    new ToCopK[:|:[H, FXNil]] {
      type Cop[t] = In1[H, t]
    }

  implicit def two[H[_], H2[_]]: ToCopK.Aux[:|:[H, :|:[H2, FXNil]], In2[H, H2, ?]] =
    new ToCopK[:|:[H, :|:[H2, FXNil]]] {
      type Cop[t] = In2[H, H2, t]
    }

  implicit def three[H[_], H2[_], H3[_]]: ToCopK.Aux[:|:[H, :|:[H2, :|:[H3, FXNil]]], In3[H, H2, H3, ?]] =
    new ToCopK[:|:[H, :|:[H2, :|:[H3, FXNil]]]] {
      type Cop[t] = In3[H, H2, H3, t]
    }

  // implicit def head[H[_], T <: FX, C[_] <: CoproductK[_]](
  //   implicit next: ToCopK[T, C]
  // ): ToCopK[:|:[H, T], ConsK[H, C, ?]] = new ToCopK[:|:[H, T], ConsK[H, C, ?]] {}
}

trait LowerToCopK {

  implicit def rec[H[_], T <: FX, C[_] <: CoproductK[_], O[_] <: CoproductK[_]](
    implicit
      next: ToCopK.Aux[T, C]
    , prep: PrependHK.Aux[H, C, O]
  ): ToCopK.Aux[:|:[H, T], O] =
    new ToCopK[:|:[H, T]] {
      type Cop[t] = O[t]
    }

  // implicit def four[H[_], H2[_], H3[_], T <: FX, C[_] <: CoproductK[_]](
  //   implicit next: ToCopK.Aux[T, C]
  // ): ToCopK.Aux[:|:[H, :|:[H2, :|:[H3, T]]], AppendK[In3[H, H2, H3, ?], C, ?]] =
  //   new ToCopK[:|:[H, :|:[H2, :|:[H3, T]]]] {
  //     type Cop[t] = AppendK[In3[H, H2, H3, ?], C, t]
  //   }

  // implicit def consk[H[_], T <: FX, C[_] <: CoproductK[_]](
  //   implicit next: ToCopK.Aux[T, C]
  // ): ToCopK.Aux[:|:[H, T], ConsK[H, C, ?]] =
  //   new ToCopK[:|:[H, T]] {
  //     type Cop[t] = ConsK[H, C, t]
  //   }

  implicit def merge[T1 <: FX, T2 <: FX, C1[_] <: CoproductK[_], C2[_] <: CoproductK[_]](
    implicit
      toCopK1: ToCopK.Aux[T1, C1]
    , toCopK2: ToCopK.Aux[T2, C2]
  ): ToCopK.Aux[:||:[T1, T2], AppendK[C1, C2, ?]] =
    new ToCopK[:||:[T1, T2]] {
      type Cop[t] = AppendK[C1, C2, t]
    }

}

trait SubFX[C[_] <: CoproductK[_], F <: FX] {
  type Cop[_] <: CoproductK[_]

  val sub: SubCop[C, Cop]
}

object SubFX {

  def apply[C[_] <: CoproductK[_], F <: FX](implicit subfxfx: SubFX[C, F]) = subfxfx

  implicit def subfx[C[_] <: CoproductK[_], F <: FX, FC[_] <: CoproductK[_]](
    implicit
      toCopK: ToCopK.Aux[F, FC]
    , sub0: SubCop[C, FC]
  ) = new SubFX[C, F] {
    type Cop[t] = FC[t]
    val sub = sub0
  }

}


/*
trait SubFX[H[_], F <: FX] {
  type Cop[_] <: CoproductK[_]

  val sub: SubCop[ConsK[H, CNilK, ?], Cop]
}

object SubFX /*extends SubFXLow*/ {
  
  type Aux[H[_], F <: FX, Out0[_] <: CoproductK[_]] = SubFX[H, F] { type Cop[t] = Out0[t] }

  def apply[H[_], F <: FX](implicit subfx: SubFX[H, F]) = subfx

  implicit def subfx[H[_], F <: FX, FC[_] <: CoproductK[_]](
    implicit
      toCopK: ToCopK[F, FC]
    , sub0: SubCop[ConsK[H, CNilK, ?], FC]
  ): SubFX.Aux[H, F, FC] = new SubFX[H, F] {
    type Cop[t] = FC[t]
    val sub = sub0
  }

  /*implicit def subfx[H[_], F <: FX, C[_] <: CoproductK[_]](
    implicit toCopK: ToCopK[F, C]
  ): SubFX.Aux[H, :|:[H, F], ConsK[H, C, ?]] = new SubFX[H, :|:[H, F]] {
    type Cop[t] = ConsK[H, C, t]
    val sub = SubCop[ConsK[H, CNilK, ?], ConsK[H, C, ?]]
  }

  implicit def subfx1[H[_], K[_], F1 <: FX, F2 <: FX, Out[_] <: CoproductK[_]]
  (
    implicit
      sub0: SubCop[ConsK[H, CNilK, ?], ConsK[K, F1#Cop, ?]]
    , merge: MergeCopHK.Aux[ConsK[K, F1#Cop, ?], F2#Cop, Out]
  ): SubFX.Aux[H, :||:[:|:[K, F1], F2], Out] = new SubFX[H, :||:[:|:[K, F1], F2]] {
    type Cop[t] = Out[t]
    val sub = new SubCop[ConsK[H, CNilK, ?], Cop] {
      def apply[A](l: ConsK[H, CNilK, A]): Cop[A] = merge.fromLeft(sub0(l))
    }
  }

  implicit def subfx2[H[_], K[_], F1 <: FX, F2 <: FX, Out[_] <: CoproductK[_]]
  (
    implicit
      sub0: SubCop[ConsK[H, CNilK, ?], F2#Cop]
    , merge: MergeCopHK.Aux[ConsK[K, F1#Cop, ?], F2#Cop, Out]
  ): SubFX.Aux[H, :||:[:|:[K, F1], F2], Out] = new SubFX[H, :||:[:|:[K, F1], F2]] {
    type Cop[t] = Out[t]
    val sub = new SubCop[ConsK[H, CNilK, ?], Cop] {
      def apply[A](l: ConsK[H, CNilK, A]): Cop[A] = merge.fromRight(sub0(l))
    }
  }*/
}

// trait SubFXLow {

  // implicit def subfxNext[H[_], K[_], F <: FX]
  // (
  //   implicit next: SubFX[H, F]
  // ): SubFX.Aux[H, :|:[K, F], ConsK[K, next.Cop, ?]] = new SubFX[H, :|:[K, F]] {
  //   type Cop[t] = ConsK[K, next.Cop, t]
  //   val sub = new SubCop[ConsK[H, CNilK, ?], ConsK[K, next.Cop, ?]] {
  //     def apply[A](l: ConsK[H, CNilK, A]): ConsK[K, next.Cop, A] = Inrk(next.sub(l))
  //   }
  // }

// }


*/