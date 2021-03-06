(ns alc.enum-repls.impl.sys
  (:require
   [clojure.string :as cs]))

(set! *warn-on-reflection* true)

(def windows?
  (cs/starts-with? (System/getProperty "os.name")
    "Windows"))
