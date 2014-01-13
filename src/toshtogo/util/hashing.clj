(ns toshtogo.util.hashing
(:import [com.google.common.hash Hashing Hasher]
         [java.nio ByteBuffer]
         [java.io ByteArrayOutputStream ByteArrayInputStream InputStream Reader]))

(defn add-to-hash!
  [^Hasher hasher ^InputStream input-stream]
  (with-open [input-stream input-stream]
    (let [#^"[B" bytes (byte-array 1024)
          read-bytes (atom 0)]
      (while (not= -1 @read-bytes)
        (reset! read-bytes (.read input-stream bytes))
        (when (not= -1 @read-bytes)
          (.putBytes hasher bytes))))

    hasher))

(defn murmur!
  "Consumes stream"
  [^InputStream s]
  (let [^Hasher hasher (.newHasher (Hashing/murmur3_128))
        ^Hasher hasher (add-to-hash! hasher s)
        hash-code      (.hash hasher)
        hash           (.asBytes hash-code)
        buffer         (ByteBuffer/allocate 16)]
    (doto buffer
      (.put hash 0 (count hash))
      (.flip))
    (let [first (.getLong buffer)
          second (.getLong buffer)]
      [first second])))
