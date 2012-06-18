(ns clj-msgpack.pack
  (:require [clojure.java.io :as io])
  (:import org.msgpack.packer.Packer)
  (:import org.msgpack.MessagePack))

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
