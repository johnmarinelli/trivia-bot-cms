(ns trivia-cms.core
  (:require [trivia-cms.handler :refer :all]
            [trivia-cms.api.api-frontend :refer :all]
            [ring.adapter.jetty :as ring])
  (:gen-class))


(defn -main []
  (ring/run-jetty app {:port (Integer. (or (System/getenv "PORT") "8080"))
                       :join? false}))
