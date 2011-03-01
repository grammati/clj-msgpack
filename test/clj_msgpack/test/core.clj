(ns clj-msgpack.test.core
  (:use [clj-msgpack.core] :reload)
  (:use [clojure.test])
  (:use [clojure.java.io :only [input-stream output-stream]]))

(defn- round-trip [obj]
  (let [ba (pack obj)]
    (-> ba unpack first)))

(deftest test-round-trip
  (doseq [obj [nil
               true false
               0 1 -1 Long/MAX_VALUE Long/MIN_VALUE
               (BigInteger/valueOf Long/MAX_VALUE)
               BigInteger/ONE
               BigInteger/TEN
               10
               1000
               10000
               100000
               1000000
               10000000
               100000000
               ""
               "Hello"
               "Unicode: \u2345"
               "\t\n\r"
               []
               [nil true false ["foo"]]
               {}
               {"foo" "bar"}
               {"a" "b" "c" "d"}
               {"a" []}
               {23 "a" 44 "b"}
               {23 ["x" "y"] 44  {"a" -7}}
               {23 ["x" "y"] nil {"a" -7}}
               ; lists come back as vectors, but still compare as equal
               (list "foo" 28838272 (list {"blah" "bloo"} nil))
               ]]
    ;(prn obj)
    ;(prn (round-trip obj))
    (is (= obj (round-trip obj)))))

(deftest test-pack-to-file
  (let [data-in [{"yo" "dawg" 147 [true false]} "kldjfld" "kdajfkd" 37447]]
    (with-open [f (output-stream "./temp.dat")]
      (apply pack-into f data-in))
    (let [data-out (unpack "./temp.dat")]
      (is (= data-in data-out)))))

