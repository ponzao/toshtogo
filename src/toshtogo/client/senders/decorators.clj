(ns toshtogo.client.senders.decorators
  (:import (toshtogo.client.senders SenderException))
  (:require [toshtogo.util.core :refer [debug exponential-backoff]]
            [toshtogo.util.json :as json]
            [toshtogo.client.util :refer [until-successful-response nil-on-404]]
            [toshtogo.client.senders.protocol :refer :all]))

(defn following-sender
  ([decorated]
   (following-sender decorated
                    (fn [sender resp] [sender resp]
                      (if (= 303 (:status resp))
                        (GET sender (get-in resp [:headers "Location"]))
                        resp))))
  ([decorated follow]
   (reify
     Sender
     (POST! [this location message]
       (follow this (POST! decorated location message)))

     (PUT! [this location message]
       (follow this (PUT! decorated location message)))

     (GET [this location]
       (follow this (GET decorated location))))))

(defn json-sender [decorated]
  (reify
    Sender
    (POST! [this location message]
      (json/decode (:body (POST! decorated location message))))

    (PUT! [this location message]
      (json/decode (:body (PUT! decorated location message))))

    (GET [this location]
      (json/decode (:body (GET decorated location))))))

(defn debug-sender [should-debug decorated]
  (if should-debug
    (reify Sender
      (POST! [this location message]
        (debug "POST RESPONSE" (apply POST! decorated (debug "POST!" [location message]))))
      (PUT! [this location message]
        (debug "PUT RESPONSE" (apply PUT! decorated (debug "PUT!" [location message]))))
      (GET [this location]
        (println "GET " location)
        (debug "GET RESPONSE" (GET decorated location))))
    decorated))

(defn retry-sender
  [decorated & {:keys [error-fn timeout]
                :or {error-fn (constantly nil)}}]

  (let [retry-opts {:interval-fn (partial exponential-backoff 5000)
                    :error-fn    error-fn
                    :timeout     timeout}]
    (reify Sender
      (POST! [this location message]
        (until-successful-response retry-opts (POST! decorated location message)))
      (PUT! [this location message]
        (until-successful-response retry-opts (PUT! decorated location message)))
      (GET [this location]
        (until-successful-response retry-opts (GET decorated location))))))

(defn nil-404
  [decorated]
  (reify Sender
    (POST! [this location message]
      (nil-on-404 (POST! decorated location message)))
    (PUT! [this location message]
      (nil-on-404 (PUT! decorated location message)))
    (GET [this location]
      (nil-on-404 (GET decorated location)))))

(defn default-decoration [sender & {:keys [error-fn timeout debug] :or {debug false} :as opts}]
  (debug-sender
    debug
    (json-sender
      (nil-404
        (following-sender
          (apply retry-sender sender (flatten (seq (select-keys opts [:error-fn :timeout])))))))))