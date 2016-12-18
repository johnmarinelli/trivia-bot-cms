(ns trivia-cms.api.user-login
  (:require [trivia-cms.models.user :as user]
            [trivia-cms.trailing-slash-middleware :refer [trailing-slash-middleware]]
            [ring.util.response :refer [response redirect]]
            [ring.middleware.json :refer [wrap-json-params]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [buddy.hashers :as hashers]
            [compojure.core :refer :all]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.params :refer [wrap-params]]))

(defn login [username password session]
  (if-let [u (user/check-user-password username password)]
    (let [user-id (.toString (:_id u))
          uuid (.toString (java.util.UUID/randomUUID))]
      (user/set-token {:_id user-id} uuid)
      (user/set-modified-at {:_id user-id})
      (-> (response "1")
          (update :headers #(merge {"Set-Cookie" (str "token=" uuid)
                                    "Username" username} %))))
    (response "0")))

(declare is-authenticated)

(defn logout [username]
  (let [token-removed (not (nil? (user/remove-token username)))]
    (user/set-modified-at {:username username})
    (if token-removed
      (response "1")
      (response "Logout failed."))))

(defn is-authenticated [{cookies :cookies :as req}]
  (let [token (:value (get cookies "token"))
        username (:value (get cookies "username"))
        user (user/find-user username)
        user-token (:token user)] 
    (and 
     (not 
      (some nil? [token username user user-token])) 
     (= token user-token))))

(defroutes user-login-routes 
  (POST "/login" request
        (let [form-params (:params request)
              session (:session request)]
          (login (:username form-params) (:password form-params) session)))

  (POST "/logout" request
        (if (is-authenticated request)
          (logout (-> request :cookies (get "username") :value))
          (response "0"))))

(def user-login
  (->
   user-login-routes
   (wrap-session)
   (wrap-defaults api-defaults)
   (wrap-json-params)))
