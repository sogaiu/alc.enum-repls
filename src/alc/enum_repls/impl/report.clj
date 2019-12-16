(ns alc.enum-repls.impl.report
  (:require
   [alc.enum-repls.impl.parse :as aei.p]
   [clojure.java.io :as cji]))

(set! *warn-on-reflection* true)

(defn paths-eq?
  [path-a path-b]
  (= (.getCanonicalPath (cji/file path-a))
    (.getCanonicalPath (cji/file path-b))))

(defn accept->type
  [accept]
  (get {"clojure.core.server/repl" "clojure-socket-repl"
        "clojure.core.server/io-prepl" "clojure-socket-prepl"
        "clojure.core.server/remote-prepl" "clojure-remote-prepl"}
    accept :unknown-type))

(defn make-tsv-row
  [fields]
  (apply str
    (interpose "\t" fields)))

(defn report-repls
  ([user-dirs repls]
   (report-repls user-dirs repls nil))
  ([user-dirs repls path]
   (doseq [[pid repls-coll] repls]
     (let [user-dir (get user-dirs pid)]
       (when (or (nil? path)
               (paths-eq? path user-dir))
         (cond
           (vector? repls-coll)
           (doseq [repl-port repls-coll]
             (println (make-tsv-row
                        [pid repl-port "boot-socket-repl" user-dir])))
           ;;
           (map? repls-coll)
           (doseq [[_ value] repls-coll]
             ;; key: "clojure.server.repl"
             ;; value: "{\\:port,3579,\\:accept,clojure.core.server/repl}"
             (when-let [[port accept] (aei.p/parse-clojure-server-str value)]
               (when (or port accept)
                 (println (make-tsv-row
                            [pid port (accept->type accept) user-dir])))))
           ;;
           :else
           (println (str "unexpected repls-coll: " repls-coll))))))))

(defn report-nrepls
  ([user-dirs nrepls]
   (report-nrepls user-dirs nrepls nil))
  ([user-dirs nrepls path]
   (doseq [[pid nrepl-ports] nrepls]
     (let [user-dir (get user-dirs pid)]
       (when (or (nil? path)
               (paths-eq? path user-dir))
         (doseq [nrepl-port nrepl-ports]
           (println (make-tsv-row
                      [pid nrepl-port "nrepl" user-dir]))))))))

(defn report-shadow-repls
  ([user-dirs shadow-repls]
   (report-shadow-repls user-dirs shadow-repls nil))
  ([user-dirs shadow-repls path]
   (doseq [[pid ports] shadow-repls]
     (let [user-dir (get user-dirs pid)]
       (when (or (nil? path)
               (paths-eq? path user-dir))
         (doseq [port ports]
           (println (make-tsv-row
                      [pid port "shadow-cljs-repl" user-dir]))))))))
