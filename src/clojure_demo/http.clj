(ns clojure-demo.http
  (:require
    [clj-simple-router.core :as router]
    [datomic.api :as d]
    [org.httpkit.server :as httpkit]
    [ring.middleware.params :refer [wrap-params]]
    [ring.middleware.resource :refer [wrap-resource]]
    [rum.core :as rum])
  (:import (java.time LocalDateTime ZoneId ZoneOffset))
  (:import (java.text SimpleDateFormat)))

(defn stop! [server]
  (some-> server httpkit/server-stop! deref))

(def ^{:init/tags #{::httpkit/config}} config
  {:port 8080
   :legacy-return-value? false})

(defn server
  "The actual HTTP server.

  Note that HTTP Kit doesn't actually support HTTPS. You'll want to set up
  some reverse proxy for that, or perhaps switch to a different HTTP server."
  {:init/tags #{:http/server}
   :init/inject [:ring/handler ::httpkit/config]
   :init/stop-fn #'stop!}
  [handler config]
  (httpkit/run-server handler config))

(defn middleware
  "Ring middleware to be applied to the handler.
  Here is the appropriate place to address cross-cutting concerns, such as
  authentication, making the database available to the handler, etc."
  {:init/tags #{:ring/middleware}
   :init/inject [:datomic/conn]}
  [conn]
  (fn [base-handler]
    (-> base-handler
        (wrap-resource "css")
        ((fn [handler]
           (fn [req]
             (-> req
                 (update :form-params update-keys keyword)
                 handler))))
        (wrap-params)
        ((fn [handler]
           (fn [req]
             (handler (assoc req :datomic {:conn conn
                                           :db (d/db conn)}))))))))

(defn handler
  "Constructs the actual handler for the requests.
  Uses the routing information and any additional middleware."
  {:init/tags #{:ring/handler}
   :init/inject [::routes :ring/middleware]}
  [routes middleware]
  (-> routes
      router/router
      middleware))

(def reminder-buttons
  (list
   [:div {:style {:display "inline"}}
    [:form {:action "/short-term"}
     [:button "Short Term Reminders"]]
    [:form {:action "/long-term"}
     [:button "Long Term Reminders"]]]))

(defn string->date
  [str-date]
  (prn str-date)
  (-> (SimpleDateFormat/new "yyyy-MM-dd") (.parse str-date)))

(defn add-reminder
  [conn {:keys [title until]} current-date]
  @(d/transact conn
               [{:reminder/id (d/squuid)
                 :reminder/title title
                 :reminder/created-at current-date
                 :reminder/until (string->date until)}]))

(defn delete-reminder
  [conn id]
  @(d/transact conn [[:db/retractEntity [:reminder/id id]]]))

(defn add-reminder-component
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

(defn render-reminder
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

(defn add-days [date days]
  (-> date
      .toInstant
      (LocalDateTime/ofInstant (ZoneId/of "UTC"))
      (.plusDays days)
      (.toInstant ZoneOffset/UTC)
      java.util.Date/from))

(defn short-term-reminders
  [path db current-date]
  (render-reminders path db '[:find [(pull ?reminder [:reminder/id :reminder/title :reminder/created-at :reminder/until]) ...]
                              :in $ ?current-date
                              :where
                              [?reminder :reminder/until ?until]
                              [(clojure-demo.http/add-days ?current-date 5) ?final-date]
                              [(< ?until ?final-date)]] current-date))

(defn long-term-reminders
  [path db current-date]
  (render-reminders path db '[:find [(pull ?reminder [:reminder/id :reminder/title :reminder/created-at :reminder/until]) ...]
                              :in $ ?current-date
                              :where
                              [?reminder :reminder/until ?until]
                              [(clojure-demo.http/add-days ?current-date 5) ?final-date]
                              [(>= ?until ?final-date)]] current-date))

(def ^:init/name routes
  "Here is where we define the route-specific handlers.
  Since we're using clj-simple-router, this is just a map."
  {"GET /"
   (fn [{{:keys [db]} :datomic}]
     {:status 200
      :body
      (let [current-date (new java.util.Date)]
      (rum/render-static-markup
       (concat
        (common-header "/")
        (list [:h2 {:style {:text-align "center"}} "All Reminders"])
        (short-term-reminders "/" db current-date)
        (long-term-reminders "/" db current-date))))})
   "GET /short-term"
   (fn [{{:keys [db]} :datomic}]
     {:status 200
      :body
      (rum/render-static-markup
       (concat
        (common-header "/short-term")
        (list [:h2 {:style {:text-align "center"}} "Short Term Reminders"])
        (short-term-reminders "/short-term" db (new java.util.Date))))})
   "GET /long-term"
   (fn [{{:keys [db]} :datomic}]
     {:status 200
      :body
      (rum/render-static-markup
       (concat
        (common-header "/long-term")
        (list [:h2 {:style {:text-align "center"}} "Long Term Reminders"])
        (long-term-reminders "/long-term" db (new java.util.Date))))})
   "POST /add-reminder"
   (fn [{{:keys [conn]} :datomic
         {:keys [path] :as body} :form-params}]
     (prn body)
     (add-reminder conn body (new java.util.Date))
     {:status 302
      :headers {"Location" path}
      :body ""})
   "POST /delete-reminder/*"
   (fn [{{:keys [conn]} :datomic
         {:keys [path]} :form-params
         [id] :path-params}]
     (delete-reminder conn (parse-uuid id))
     {:status 302
      :headers {"Location" path}
      :body ""})})
