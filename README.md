# clj-msgpack #

This is a Clojure wrapper for the [MessagePack](http://msgpack.org/)
library.

It allows you serialize and deserialize simple clojure objects to and
from  a well-defined binary format that is portable to many other
programming languages.

## Example ##

Put some objects in a file:

    (require '[clj-msgpack.core :as mp])
    (use '[clojure.java.io :only [output-stream]])
    
    (def data [nil true false {"yo" "dawg"} ["foo" "bar"]])
    (with-open [f (output-stream "./temp.dat")]
      (mp/pack-into f "Hello" 23)
      (apply mp/pack-into f data))
    

Pull them back out somewhere else (eg: ruby):

    require 'msgpack'
    u = MessagePack::Unpacker.new
    u.feed( File.read('./temp.dat') )
    u.each {|ob| p ob}
      "Hello"
      23
      nil
      true
      false
      {"yo"=>"dawg"}
      ["foo", "bar"]
    

## Why? ##

Why would you want to use this instead of, say, JSON, or Java
serialization? Honestly, I'm not sure yet :) The serialization format
is portable to many languages, and it's quite compact, especially if
you are serializing a lot of numbers. For stringy data, it's probably
not much smaller than JSON. At some point I'll do some benchmarks to
see how small and/or fast this is compared to some of the
alternatives.

