package net.elytrium.limboapi.api.world.item.datacomponent.type.data;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public sealed interface Either<L, R> permits Either.Left, Either.Right {

  <T> T map(Function<? super L, ? extends T> l, Function<? super R, ? extends T> r);

  Either<L, R> ifLeft(Consumer<? super L> consumer);

  Either<L, R> ifRight(Consumer<? super R> consumer);

  Optional<L> left();

  Optional<R> right();

  static <L, R> Either<L, R> left(L value) {
    return new Left<>(value);
  }

  static <L, R> Either<L, R> right(R value) {
    return new Right<>(value);
  }

  static <U> U unwrap(Either<? extends U, ? extends U> either) {
    return either.map(Function.identity(), Function.identity());
  }

  record Left<L, R>(L value) implements Either<L, R> {

    @Override
    public <T> T map(Function<? super L, ? extends T> l, Function<? super R, ? extends T> r) {
      return l.apply(this.value);
    }

    @Override
    public Either<L, R> ifLeft(Consumer<? super L> consumer) {
      consumer.accept(this.value);
      return this;
    }

    @Override
    public Either<L, R> ifRight(Consumer<? super R> consumer) {
      return this;
    }

    @Override
    public Optional<L> left() {
      return Optional.of(this.value);
    }

    @Override
    public Optional<R> right() {
      return Optional.empty();
    }
  }

  record Right<L, R>(R value) implements Either<L, R> {

    @Override
    public <T> T map(Function<? super L, ? extends T> l, Function<? super R, ? extends T> r) {
      return r.apply(this.value);
    }

    @Override
    public Either<L, R> ifLeft(Consumer<? super L> consumer) {
      return this;
    }

    @Override
    public Either<L, R> ifRight(Consumer<? super R> consumer) {
      consumer.accept(this.value);
      return this;
    }

    @Override
    public Optional<L> left() {
      return Optional.empty();
    }

    @Override
    public Optional<R> right() {
      return Optional.of(this.value);
    }
  }
}
