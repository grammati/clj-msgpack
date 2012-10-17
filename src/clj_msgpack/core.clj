(ns clj-msgpack.core
  (:require [clojure.java.io :as io]) 
  (:import (org.msgpack MessagePack)
           (org.msgpack.packer Packer)
           (org.msgpack.unpacker Unpacker)
           (org.msgpack.type ArrayValue BooleanValue MapValue RawValue Value
                             FloatValue IntegerValue NilValue NumberValue
                             ;; These are internal and are probably a source of cross-version
                             ;; headaches.
                             BigIntegerValueImpl IntValueImpl LongValueImpl
                             DoubleValueImpl FloatValueImpl)))

;;; Packing

(defprotocol Packable
  "Serialize the object to the packer."
  (pack-me [obj packer] "Serialize the object into the packer."))

(extend-protocol Packable
  nil
  (pack-me [_ ^Packer packer]
    (.writeNil packer))

  clojure.lang.Keyword
  (pack-me [kw ^Packer packer]
    (.write packer ^String (str \: (name kw)))) ; not round-trippable, but
                                                ; better than nothing.

  clojure.lang.Symbol
  (pack-me [sym ^Packer packer]
    (.write packer ^String (name sym)))

  clojure.lang.Sequential
  (pack-me [s ^Packer packer]
    (.writeArrayBegin packer (count s))
    (doseq [item s]
      (pack-me item packer))
    (.writeArrayEnd packer))

  clojure.lang.IPersistentMap
  (pack-me [m ^Packer packer]
    (.writeMapBegin packer (count m))
    (doseq [[k v] m]
      (pack-me k packer)
      (pack-me v packer))
    (.writeMapEnd packer))

  Object
  (pack-me [obj ^Packer packer]
    (.write packer ^Object obj)))

(defprotocol ToPacker
  (to-packer [obj]))

(extend-protocol ToPacker
  ;Convert an object into an org.msgpack.Packer instance.
  Packer
  (to-packer [p] p)
  
  Object
  (to-packer [obj]
    (let [mpacker (MessagePack.)]
      (.createPacker mpacker (io/output-stream obj)))))

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
  (let [p (.createBufferPacker (MessagePack.))]
    (apply pack-into p objs)
    (.toByteArray p)))


;;; Unpacking

(defprotocol Unwrapable
  (unwrap [msgpack-obj]
          "Unwrap one of the funky wrapper objects that msgpack uses."))

(def ^:dynamic *keywordize-strings* 
  "When true, unpack will convert strings that start with a colon into keywords."
  false)

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
  (unwrap [o]
    (let [v (.getString o)]
      (if (and *keywordize-strings* (.startsWith v ":"))
        (keyword (.substring v 1))
        v))))

(defn unpack [from]
  (let [is (io/input-stream from) ; hmmm, can't use with-open here...
        u (.createUnpacker (MessagePack.) is)]
    (map unwrap u)))

