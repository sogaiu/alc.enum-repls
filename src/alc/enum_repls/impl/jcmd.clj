(ns alc.enum-repls.impl.jcmd
  (:require
   [clojure.java.shell :as cjs]
   [clojure.string :as cs]))

(set! *warn-on-reflection* true)

;; sample `jcmd` output
;;
;; 15651 clojure.main -e (println,"socket-repl-at-port:3579") -r
;; 31891 clojure.main -r
;; 20213 clojure.main -e (println,"socket-repl-at-port:2357") -r
;; 8231 jdk.jcmd/sun.tools.jcmd.JCmd
;; 18041 clojure.main -m leiningen.core.main repl
;; 18091 clojure.main -i /tmp/form-init1221942576716107350.clj
;; 32287 /home/user/.vscode-oss/extensions/redhat.java-0.54.2/server/plugins/org.eclipse.equinox.launcher_1.5.600.v20191014-2022.jar -configuration /home/user/.vscode-oss/extensions/redhat.java-0.54.2/server/config_linux -data /home/user/.config/VSCodium/User/workspaceStorage/2b069293095f7ff3f9fda202a39d5744/redhat.java/jdt_ws
(defn parse-jcmd
  [jcmd-res pred]
  (->> (cs/split-lines jcmd-res) 
    (keep (fn [line]
            (when-let [[k v] (cs/split line #" " 2)]
              (pred [k v]))))
    (into {})))
;; {"15651" "clojure.main -e (println,"socket-repl-at-port:3579") -r"
;;  "31891" "clojure.main -r"
;;  "8231" "jdk.jcmd/sun.tools.jcmd.JCmd"
;;  ...}

(defn find-jvms
  ([jcmd-path]
   (find-jvms jcmd-path
     (fn [[k v]]
       [k v])))
  ([jcmd-path pred]
   (let [{:keys [_ :exit :out]} (cjs/sh jcmd-path)]
     (when (= 0 exit)
       (parse-jcmd out pred)))))

;; XXX: skip leiningen process; likely there is corr process that isn't skipped
(defn clojure-matcher
  [[key value]]
  (when (and (cs/starts-with? value "clojure.main")
          (not (cs/starts-with? value "clojure.main -m leiningen.core.main ")))
    [key value]))

;; sample `jcmd <pid> VM.system_properties` output
;;
;; 15651:
;; #Thu Dec 12 09:47:32 UTC 2019
;; java.runtime.name=OpenJDK Runtime Environment
;; sun.boot.library.path=/usr/lib/jvm/java-8-openjdk/jre/lib/amd64
;; java.vm.version=25.232-b09
;; ...
;; java.class.path=...
;; ...
;; sun.java.command=clojure.main -e (println,"socket-repl-at-port\:3579") -r
;; ...
;; clojure.server.repl={\:port,3579,\:accept,clojure.core.server/repl}
;; ...
;;
;; pred takes a 2-element vector [key value] and should return
;; a 2-element vector (to be collected into a map) or nil
(defn extract-props
  ([jcmd-res]
   (extract-props jcmd-res
     (fn [[k v]]
       [k v])))
  ([jcmd-res pred]
   (->> (cs/split-lines jcmd-res)
     (keep (fn [line]
             (when (cs/includes? line "=")
               (pred (cs/split line #"=" 2)))))
     (into {}))))
;; {"clojure.server.repl" "{\\:port,3579,\\:accept,clojure.core.server/repl}"
;;  "java.vm.version" "25.2332-b09"
;;  ...}

;; jcmd appears to escape colons, possibly because of Properties:
;;   https://stackoverflow.com/a/38436788
(defn unescape
  [value]
  (cs/replace value
    "\\:" ":"))

;; sample key: "clojure.server.repl"
(defn clojure-server-matcher
  [[key value]]
  (when (or (= "user.dir" key)
          (re-find #"^clojure\.server\.(.*)" key))
    [key (unescape value)]))

(defn jcmd-sys-prop
  [jcmd-path pid]
  (try
    (cjs/sh jcmd-path pid "VM.system_properties")
    (catch Exception e
      ;; XXX: do something better than this?
      (println e)
      nil)))

(defn find-repls
  [jcmd-path clojures]
  (->> clojures
    (keep (fn [[pid _]]
            (let [{:keys [_ :exit :out]}
                  (jcmd-sys-prop jcmd-path pid)]
              (when (= 0 exit)
                (let [repls (extract-props out clojure-server-matcher)]
                  (when (< 0 (count repls))
                    [pid repls]))))))
    (into {})))

;; XXX: complected?
(defn boot-matcher
  [[key value]]
  (when (or (= "user.dir" key)
          ;; XXX: just use equals?
          (re-find #"^boot.class.path" key))
    [key (unescape value)]))

(defn find-boots
  [jcmd-path maybe-boots]
  (->> maybe-boots
    (keep (fn [[pid _]]
            (let [{:keys [_ :exit :out]}
                  (jcmd-sys-prop jcmd-path pid)]
              (when (= 0 exit)
                (let [boots (extract-props out boot-matcher)]
                  (when (< 0 (count boots))
                    [pid boots]))))))
    (into {})))
