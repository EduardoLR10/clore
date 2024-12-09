(ns clojure-demo.renderer
  (:require
    [clj-simple-router.core :as router]
    [clojure-demo.datomic :as datomic]
    [datomic.api :as d]
    [org.httpkit.server :as httpkit]
    [ring.middleware.params :refer [wrap-params]]
    [ring.middleware.resource :refer [wrap-resource]]
    [rum.core :as rum])
  (:import (java.time LocalDateTime ZoneId ZoneOffset))
  (:import (java.text SimpleDateFormat)))

(def reminder-buttons
  (list
   [:div {:style {:display "inline"}}
    [:form {:action "/short-term"}
     [:button "Short Term Reminders"]]
    [:form {:action "/long-term"}
     [:button "Long Term Reminders"]]]))

(defn ^:private add-reminder-component
  [path]
  (list
   [:form {:method "post" :action "/add-reminder"}
    [:div {:style {:display "inline"}}
     [:div {:style {:display "inline"}}
      [:label {:for "title"} "Reminder Title:"]
      [:input {:type "text" :name "title" :required ""}]]
     [:div {:style {:display "inline"}}
      [:label {:for "until"} "Until then:"]
      [:input {:type "date" :name "until" :required ""}]]
     [:div {:style {:display "inline"}}
      [:input {:type "hidden" :name "path" :required "" :value path}]]
     [:div {:style {:display "inline"}}
      [:input {:type "submit" :value "Create"}]]]]))

(defn common-header
  [path]
  (concat
   (list
   [:head
    [:link {:rel "stylesheet" :href "reset.css"}]
    [:link {:rel "stylesheet" :href "main.css"}]]
   [:h1 [:a {:href "/"} "Clojure Demonstration"]])
   reminder-buttons
   (list [:br] [:br])
   (list
    [:div
     (add-reminder-component path)])))

(defn ^:private render-reminder
  [path {:reminder/keys [id title created-at until]}]
  [:li
   [:div {:style {:display "inline"}}
    title " "
    created-at " "
    until "   "
    [:div {:style {:display "inline-block"}}
    [:form {:method "post" :action (str "/delete-reminder/" id)}
     [:input {:type "hidden" :name "path" :required "" :value path}]
     [:button "Delete"]]]]])

(defn render-reminders
  [path db query current-date]
  (prn (d/q query db current-date))
  (list
   [:ul
   (map (partial render-reminder path) (d/q query db current-date))]))

(defn short-term-reminders
  [path db current-date]
  (render-reminders path db datomic/short-term-query current-date))

(defn long-term-reminders
  [path db current-date]
  (render-reminders path db datomic/long-term-query current-date))

(defn title [text]
  [:h2 {:style {:text-align "center"}} text])
