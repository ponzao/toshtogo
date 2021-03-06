(ns toshtogo.server.core
  (:require [toshtogo.server.migrations.run :refer [run-migrations!]]
            [toshtogo.server.handler :refer [app]]
            [toshtogo.server.logging :refer :all]
            [ring.adapter.jetty :refer [run-jetty]]
            [watchtower.core :as watcher])
  (:import [toshtogo.server.logging SysLogger]
           [org.eclipse.jetty.server Server])
  (:gen-class))

(def dev-db {:classname   "org.postgresql.Driver"           ; must be in classpath
             :subprotocol "postgresql"
             :subname     "//localhost:5432/toshtogo"
             :user        "postgres"
             :password    "postgres"})

(defn dev-app [& {:keys [debug logger-factory db]
                  :or   {debug false
                         db    dev-db}}]
  (app db
       :debug debug
       :logger-factory (or logger-factory
                           (if debug (constantly (SysLogger.))
                                     (constantly nil)))))

(defn start! [debug port join?]
  (run-migrations! dev-db)
  (let [^Server server (run-jetty (dev-app :debug debug) {:port port :join? join?})]
    (fn [] (.stop server))))

(defn -main [& {debug "-debug"}]
  (start! (= "yes" debug) 3001 true))

(defn reload-templates! [files]
  (require 'toshtogo.server.handler :reload-all))

(defn auto-reloading-start!
  "For interactive development ONLY"
  []
  ; Reload this namespace and its templates when one of the templates changes.
  (when-not (= (System/getenv "RING_ENV") "production")
    (watcher/watcher ["src"]
                     (watcher/rate 50)                      ; poll every 50ms
                     (watcher/file-filter (watcher/extensions :html))
                     (watcher/on-change reload-templates!))))
