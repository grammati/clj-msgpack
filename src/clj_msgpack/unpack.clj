(ns clj-msgpack.unpack
  (:use (clojure.java (io :only (input-stream))))
  (:import org.msgpack.unpacker.Unpacker)
  (:import org.msgpack.MessagePack)
  (:import (org.msgpack.type ArrayValue BooleanValue MapValue RawValue Value
                             FloatValue IntegerValue NilValue NumberValue
                             ;; These are internal and are probably a source of cross-version
                             ;; headaches.
                             BigIntegerValueImpl IntValueImpl LongValueImpl
                             DoubleValueImpl FloatValueImpl)))

(defprotocol Unwrapable
  (unwrap [msgpack-obj]
          "Unwrap one of the funky wrapper objects that msgpack uses."))

(extend-protocol Unwrapable
  ;; Specialized unwraps
  BigIntegerValueImpl
  (unwrap [o] (.getBigInteger o))
  DoubleValueImpl
  (unwrap [o] (.getDouble o))
  FloatValueImpl
  (unwrap [o] (.getFloat o))
  LongValueImpl
  (unwrap [o] (.getLong o))

  ;; Non-specialized
  IntegerValue
  (unwrap [o] (.getInt o))
  ArrayValue
  (unwrap [o] (into [] (map unwrap (.getElementArray o))))
  BooleanValue
  (unwrap [o] (.getBoolean o))
  MapValue
  (unwrap [o] (into {} (map (fn [[k v]] [(unwrap k) (unwrap v)]) o)))
  NilValue
  (unwrap [o] nil)
  RawValue
  (unwrap [o] (.getString o)))

(defn unpack [from]
  (let [is (input-stream from) ; hmmm, can't use with-open here...
        u (.createUnpacker (MessagePack.) is)]
    (map unwrap u)))
