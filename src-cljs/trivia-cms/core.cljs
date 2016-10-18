(ns trivia-cms.core
  (:require [clojure.string :as string]
            [reagent.core :as r]))

(enable-console-print!)

(defonce app-state
  (r/atom
   {:contacts
    [{:key 0 :first "Ben" :last "Stiller" :email "ben.stiller@yahoo.com"}]}))

(defn update-contacts! [f & args]
  (apply swap! app-state update-in [:contacts] f args))

(defn add-contact! [contact]
  (update-contacts! conj contact))

(defn remove-contact! [contact]
  (update-contacts! (fn [cs]
                      (vec (remove #(= % contact) cs)))
                    contact))

(defn parse-contact [s]
  (let [[first last-name] (string/split s #"\s+")]
    {:key 
     (inc 
      (:key 
       (last (:contacts @app-state))))  
     :first first 
     :last last-name}))

;; UI components
(defn contact [c]
  [:li
   [:span (str (:first c) " " (:last c))]
   [:button {:on-click #(remove-contact! c)}
    "Delete"]])

(defn new-contact []
  (let [val (r/atom "")]  ; The new-contact holds the current value of the input text box as local state in the val atom. 
    (fn []
      [:div
       [:input {:type "text"
                :placeholder "Name"
                :value @val
                :on-change #(reset! val (-> % .-target .-value))}]
       [:button {:on-click #(when-let [c (parse-contact @val)]
                              (add-contact! c)
                              (reset! val ""))}
        "Add"]])))

(defn contact-list []
  [:div
   [:h1 "Contacts"]
   [:ul
    (for [c (:contacts @app-state)]
      [contact c])]
   [new-contact]])

(defn my-component []
  [:div
   [:h2 "SUP"]])

;; Render root 
(defn start []
  (r/render-component
   [contact-list]
   (.getElementById js/document "root"))
  (r/render-component
   [my-component]
   (.getElementById js/document "john")))
