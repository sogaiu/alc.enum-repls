(ns alc.enum-repls.core
  (:require
   [alc.enum-repls.impl.exit :as aei.e]
   [alc.enum-repls.impl.fs :as aei.f]
   [alc.enum-repls.impl.jcmd :as aei.j]
   [alc.enum-repls.impl.report :as aei.r]
   [alc.enum-repls.impl.sys :as aei.s]
   [alc.enum-repls.impl.which :as aei.w]))

;; TODO:
;;
;; - sync docs
;; - allow specifying path to jcmd binary
;; - work on VirtualMachine implementation
;; - option to suggest unused random port?
;; - investigate `cljs.server.node/repl`

(set! *warn-on-reflection* true)

(defn enum-repls
  [{:keys [:debug :proj-dir]}]
  (let [ctx {}
        jcmd-path (aei.w/which (str "jcmd"
                                (when aei.s/windows?
                                  ".exe")))
        _ (when-not jcmd-path
            (aei.e/exit 1 "`jcmd` not found"))
        ctx (assoc ctx :jcmd-path jcmd-path)
        jvms (aei.j/find-jvms jcmd-path)
        _ (when-not jvms
            (aei.e/exit 1
              (str "`jcmd` exited non-zero")))
        ctx (assoc ctx :jvms jvms)
        clojures (keep aei.j/clojure-matcher jvms)
        ctx (assoc ctx :clojures clojures)
        clojure-repls (aei.j/find-repls jcmd-path clojures)
        ctx (assoc ctx :clojure-repls clojure-repls)
        user-dirs (->> clojure-repls
                    (keep (fn [[pid repls-map]]
                            (when-let [user-dir (get repls-map "user.dir")]
                              [pid user-dir])))
                    (into {}))
        ctx (assoc ctx :clojure-user-dirs user-dirs)
        maybe-boots (filter (fn [[_ v]]
                              (re-find #"boot" v))
                      jvms)
        ctx (assoc ctx :maybe-boots maybe-boots)
        boots (aei.j/find-boots jcmd-path maybe-boots)
        ctx (assoc ctx :boots boots)
        boot-user-dirs (->> boots
                         (keep (fn [[pid kept-map]]
                                 (when-let [user-dir (get kept-map "user.dir")]
                                   [pid user-dir])))
                         (into {}))
        ctx (assoc ctx :boot-user-dirs boot-user-dirs)
        boot-socket-repls (aei.f/find-boot-socket-repls boot-user-dirs)
        ctx (assoc ctx :boot-socket-repls boot-socket-repls)
        user-dirs (->> (concat boot-user-dirs user-dirs)
                    (into {}))
        ctx (assoc ctx :all-user-dirs user-dirs)
        ;; XXX: may be do separately?
        repls (merge clojure-repls boot-socket-repls)
        ctx (assoc ctx :repls repls)
        nrepls (aei.f/find-nrepls user-dirs)
        ctx (assoc ctx :nrepls nrepls)
        shadow-repls (aei.f/find-shadow-repls user-dirs)
        ctx (assoc ctx :shadow-repls shadow-repls)]
    (aei.r/report-repls user-dirs repls proj-dir)
    (aei.r/report-nrepls user-dirs nrepls proj-dir)
    (aei.r/report-shadow-repls user-dirs shadow-repls proj-dir)
    (when debug ctx)))

(comment

  (tap> (enum-repls {:debug true}))

  )
