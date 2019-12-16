(ns alc.enum-repls.impl.which
  (:require
   [clojure.java.io :as cji]
   [clojure.string :as cs]))

(set! *warn-on-reflection* true)

(defn which
  [bin-name]
  ;; from taylorwood
  (let [paths
        (cs/split (System/getenv "PATH")
          (re-pattern (java.io.File/pathSeparator)))]
    (first
      (for [path (distinct paths) 
            :let [file (cji/file path bin-name)] 
            :when (.exists file)]
        (.getAbsolutePath file)))))
