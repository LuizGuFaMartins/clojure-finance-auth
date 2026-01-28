(ns build
  "Used to build projects.
   To know more, check: https://clojure.org/guides/tools_build"
  (:require [clojure.java.shell :as shell]
            [clojure.tools.build.api :as b]))

(def class-dir
  "target/classes")

(def basis
  (b/create-basis {:project "deps.edn"}))

(def uber-file
  "target/server.jar")

(defn clean [_]
      (b/delete {:path "target"}))

(defn uber [{:keys [entrypoint]}]
      (clean nil)

      (b/copy-dir {:src-dirs   ["src" "resources"]
                   :target-dir class-dir})

      (b/compile-clj {:basis     basis
                      :src-dirs  ["src"]
                      :class-dir class-dir})

      (b/uber {:class-dir class-dir
               :uber-file uber-file
               :basis     basis
               :main      entrypoint})

      ; config.edn can be overwritten on uber jar packaging.
      ; we are making sure the config.edn at the uberjar root is the desired one.
      ; this command will update a file (uf) on a already packaged jar file
      (shell/sh "jar" "uf" "target/server.jar" "-C" "target/classes" "config.edn"))
