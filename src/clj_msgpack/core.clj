(ns clj-msgpack.core
  (:import (org.msgpack Packer Unpacker)
           (org.msgpack.object ArrayType BigIntegerTypeIMPL BooleanType
                               DoubleTypeIMPL FloatTypeIMPL IntegerType
                               LongIntegerTypeIMPL MapType NilType
                               RawType ShortIntegerTypeIMPL))
  (:use (clojure.java [io :only [input-stream output-stream]])))

(defprotocol Packable
  "Serialize the object to the packer."
  (pack-me [obj packer] "Serialize the object into the packer."))

(extend-protocol Packable

  nil
  (pack-me [_ ^Packer packer]
    (.packNil packer))
  
  clojure.lang.Keyword
  (pack-me [kw ^Packer packer]
    (.pack packer (str \: (name kw)))) ; not round-trippable, but
                                       ; better than nothing.
  
  clojure.lang.Symbol
  (pack-me [sym ^Packer packer]
    (.pack packer (name sym)))

  clojure.lang.Sequential
  (pack-me [s ^Packer packer]
    (.packArray packer (count s))
    (doseq [item s]
      (pack-me item packer)))

  clojure.lang.IPersistentMap
  (pack-me [m ^Packer packer]
    (.packMap packer (count m))
    (doseq [[k v] m]
      (pack-me k packer)
      (pack-me v packer)))

  Object
  (pack-me [obj ^Packer packer]
    (.pack packer obj))
  
  )

(defprotocol ToPacker
  (to-packer [obj]))

(extend-protocol ToPacker
  ;Convert an object into an org.msgpack.Packer instance.
  Packer
  (to-packer [p] p)
  Object
  (to-packer [obj]
    (-> obj output-stream Packer.))
  )

(defn packer [dest]
  (to-packer dest))

(defn pack-into
  "Pack objects to the destination, which must be a Packer or coercible to an InputStream."
  [dest & objs]
  (let [p (packer dest)]
    (doseq [obj objs]
      (pack-me obj p))
    p))

(defn pack
  "Pack the objects into a byte array and return it."
  [& objs]
  (let [ba (java.io.ByteArrayOutputStream.)
        p (Packer. ba)]
    (apply pack-into p objs)
    (.toByteArray ba)))


(defprotocol Unwrapable
  (unwrap [msgpack-obj]
          "Unwrap one of the funky wrapper objects that msgpack uses."))

(extend-protocol Unwrapable
  ArrayType
  (unwrap [o] (into [] (map unwrap (.asArray o))))
  BigIntegerTypeIMPL
  (unwrap [o] (.asBigInteger o))
  BooleanType
  (unwrap [o] (.asBoolean o))
  DoubleTypeIMPL
  (unwrap [o] (.asDouble o))
  FloatTypeIMPL
  (unwrap [o] (.asFloat o))
  IntegerType
  (unwrap [o] (.asInt o))
  LongIntegerTypeIMPL
  (unwrap [o] (.asLong o))
  MapType
  (unwrap [o] (into {} (map (fn [[k v]] [(unwrap k) (unwrap v)]) (.asMap o))))
  NilType
  (unwrap [o] nil)
  RawType
  (unwrap [o] (.asString o))
  ShortIntegerTypeIMPL
  (unwrap [o] (.asInt o))
  )

(defn unpack [from]
  (let [is (input-stream from) ; hmmm, can't use with-open here...
        u (Unpacker. is)]
    (map unwrap u)))
