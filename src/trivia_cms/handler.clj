(ns trivia-cms.handler
  (:require [trivia-cms.api.api-frontend :refer [api]]
            [compojure.core :refer [routes]]))

(def app
  (routes api))
