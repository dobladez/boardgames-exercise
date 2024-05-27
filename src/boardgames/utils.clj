(ns boardgames.utils (:require [clojure.string :as str]))


(defn seek
  "Returns first item from coll for which (pred item) returns true.
   Returns nil if no such item is present, or the not-found value if supplied."
  {:added  "1.9" ; note, this was never accepted into clojure core
   :static true}
  ([pred coll] (seek pred coll nil))
  ([pred coll not-found]
   (reduce (fn [_ x]
             (if (pred x)
               (reduced x)
               not-found))
           not-found coll)))

(def upper-case-keyword (comp keyword str/upper-case name))

(defn replace-in-set [s old-elem new-elem]
  (if (contains? s old-elem)
    (-> s
        (disj old-elem)
        (conj new-elem))
    s))

(defmacro ttap> [& body]
  `(let [result# (do ~@body)]
     (tap> result#)
     result#))