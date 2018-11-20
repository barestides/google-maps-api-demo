(ns google-maps-api-demo.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as r]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]
            [clojure.string :as string]))

(enable-console-print!)

(defonce app-state (r/atom {:origin ""
                            :destination ""}))


(defn titleize
  [str-or-kw]
  (string/capitalize (name str-or-kw)))

(defn text-box-on-change-fn
  [state field]
  (fn [new-val]
    (swap! state assoc field (-> new-val
                                 .-target
                                 .-value))))

(defn text-box
  [state field]
  [:div.form-group
   [:label {:for field} (titleize field)]
   [:input.form-control {:id field :type "text" :value (field @state)
                                 :on-change (text-box-on-change-fn state field)}]])

(defn submit-form
  [state]
  (prn "we submitting"))

(defn location-form
  [state]
  [:form.col-sm-6 {:on-submit (fn [event]
                                ;;so the page doesn't refresh when the user submits
                                (do
                                  (.preventDefault event)
                                  (submit-form state)))}
   [text-box state :origin]
   [text-box state :destination]
   [:input.btn.btn-primary {:type :submit :value "Submit"}]])


(defn page
  [state]
  [:div.container
   [:h2 "Google Maps API CLJS Example App"]
   [:div.row
    [location-form state]]])

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  (r/render [page app-state] (.getElementById js/document "app")))
