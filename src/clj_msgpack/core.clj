(ns clj-msgpack.core
  (:import (org.msgpack Packer))
  (:use (clojure.java.io :only [input-stream])))

(defprotocol Packable
  "Serialize the object to the packer."
  (pack-me [obj packer] "Serialize the object into the packer."))

;; Most common clojure types get packed properly by default:
;; Maps, Lists, Vectors, Strings, Numbers, nil, true, false, ...
;; Handle the rest:
(extend-protocol Packable
  
  clojure.lang.Keyword
  (pack-me [kw packer]
    (.pack packer (str \: (name kw))))
  
  clojure.lang.Symbol
  (pack-me [sym packer]
    (.pack packer (name sym)))

  clojure.lang.IPersistentSet
  (pack-me [s packer]
    (doseq [item s]
      (pack-me item packer)))

  Object
  (pack-me [obj packer]
    (.pack packer obj))

  )

(defprotocol ToPacker
  (to-packer [obj]))

(extend-protocol ToPacker
  "Convert an object into an org.msgpack.Packer instance."
  Packer
  (to-packer [p] p)
  Object
  (to-packer [obj]
    (-> obj input-stream Packer.))
  )

(defn packer
  "Creat an org.msgpack.Packer instance.
   With no arguments, create an return a Packer that writes to a *out*."
  )

(defn pack
  "Pack the objects into a byte array and return it."
  [& objs]
  (let [ba (java.io.ByteArrayOutputStream.)
        p (Packer. ba)]
    (doseq [obj objs]
      (pack-to obj p))
    (.toByteArray s)))

(defn pack-into
  "Pack objects to the destination, which must be a Packer or coercible to an InputStream."
  [dest & objs]
  (let [p (packer)]
    (doseq [obj objs]
      (pack-to obj p))
    (.toByteArray s)))

(defmethod pack-to Packer))

(defn unpack [bytes]
  )
