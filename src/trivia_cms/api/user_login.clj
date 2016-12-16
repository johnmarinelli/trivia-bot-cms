(ns trivia-cms.api.user-login
  (:require [trivia-cms.models.user :as user]
            [trivia-cms.trailing-slash-middleware :refer [trailing-slash-middleware]]
            [ring.util.response :refer [response redirect]]
            [ring.middleware.json :refer [wrap-json-params]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [compojure.core :refer :all]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.params :refer [wrap-params]]))

(defn login [username password session]
  (if-let [u (user/check-user-password username password)]
    (let [uid (.toString (:_id u))]
      (user/set-token {:_id uid} "1")
      (user/set-modified-at {:_id uid})
      (-> (response "1")
          (update :headers #(merge {"Set-Cookie" (str "token=" (user/get-token uid))
                                    "Username" username} %))))
    (response "0")))

(declare is-authenticated)

(defn logout [username]
  (user/remove-token username)
  (user/set-modified-at {:username username})
  (response "1"))
(defn is-authenticated [{cookies :cookies :as req}]
  (let [token (:value (get cookies "token"))
        username (:value (get cookies "username"))
        user (user/find-user username)
        user-token (:token user)] 
    (and (not (some nil? [token username user user-token])) (= token user-token))))

(defn wrap-user [handler]
  (fn [{user-id :identity :as req}]
    (handler (assoc req :user (user/find-user user-id)))))

(defroutes user-login-routes 
  (POST "/login" request
        (let [form-params (:params request)
              session (:session request)]
          (println "Params:" form-params)
          (println "Request: " request)
          (login (:username form-params) (:password form-params) session)))

  (POST "/logout" request []
        (if (is-authenticated request)
          (logout request)
          (response "0"))))

(def user-login
  (->
   user-login-routes
   (wrap-session)
   (wrap-defaults api-defaults)


   (wrap-user)
   (wrap-json-params)))
