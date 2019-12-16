(ns alc.enum-repls.main
  (:require
   [alc.enum-repls.core :as ae.c]
   [alc.enum-repls.impl.exit :as aei.e]
   [alc.enum-repls.impl.opts :as aei.o])
  (:gen-class))

(set! *warn-on-reflection* true)

(defn -main
  [& args]
  (let [opts
        (if-let [first-str-opt (aei.o/find-first-string args)]
          {:proj-dir first-str-opt}
          {})
        opts (merge opts
               (aei.o/merge-only-map-strs args))]
    (ae.c/enum-repls opts))
  (aei.e/exit 0))
