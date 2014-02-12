(ns toshtogo.server.core
  (:require [toshtogo.server.migrations.run :refer [run-migrations!]]
            [toshtogo.server.handler :refer [app]]
            [ring.adapter.jetty :refer [run-jetty]]
            [watchtower.core :as watcher])
  (:gen-class))

(def dev-db {:classname   "org.postgresql.Driver" ; must be in classpath
                  :subprotocol "postgresql"
                  :subname     "//localhost:5432/toshtogo"
                  :user        "postgres"
                  :password    "postgres"})

(defn dev-app [& {:keys [debug] :or {debug false}}]
  (app dev-db :debug debug))

(def dev-app-instance (dev-app :debug true))

(defn -main [& args]
  (run-migrations! dev-db)
  (run-jetty (dev-app :debug true) {:port 3000}))

(defn reload-templates! [files]
  (require 'toshtogo.server.handler :reload-all))

(defn auto-reloading-start!
  "For interactive development ONLY"
  []
  ; Reload this namespace and its templates when one of the templates changes.
  (when-not (= (System/getenv "RING_ENV") "production")
    (watcher/watcher ["src"]
                     (watcher/rate 50) ; poll every 50ms
                     (watcher/file-filter (watcher/extensions :html))
                     (watcher/on-change reload-templates!))))