(ns qlkit-todo.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [qlkit.core :as ql]
            [qlkit-renderer.core :refer [transact! update-state!] :refer-macros [defcomponent]]
            [goog.dom :refer [getElement]]
            [qlkit-todo.parsers :refer [read mutate remote sync]]
            [qlkit-material-ui.core :refer [enable-material-ui!]]
            [cljs-http.client :as http :refer [post]]
            [cljs.reader :refer [read-string]]
            [goog.string :as gstring]))

(enable-console-print!)
(enable-material-ui!)

(defonce app-state (atom {}))

(defcomponent TodoItem
  (query [[:todo/text] [:db/id]])
  (render [{:keys [:todo/text] :as atts} state]
          [:list-item {:button true}
           [:list-item-text {:primary text}]
           [:list-item-secondary-action {}
            [:icon-button {}
             [:icon/cancel {:on-click (fn []
                                        (transact! [:todo/delete!])
                                        (transact! [:ui/set-ui! {:anim-type "char-out" :anim-delay 60}]))}]]]]))

(def text
  "Lorem ipsum dolor sit amet consectetur adipisicing elit. Cupiditate incidunt praesentium, rerum voluptatem in reiciendis officia harum repudiandae tempore suscipit ex ea, adipisci ab porro.")

(defn make-text [css-class animation-delay]
  (let [f (fn [i x]
            [:span {:class css-class
                    :style {:animationDelay (str (* i animation-delay) "ms")}}
             (if (= x " ") (gstring/unescapeEntities "&nbsp;") x)])]
    (into [:p] (map-indexed f text))))

(defcomponent TodoList
  (query [[:qlkit-todo/todos (ql/get-query TodoItem)]])
  (render [{:keys [:qlkit-todo/todos] :as atts} {:keys [new-todo] :as state}]
          [:div {:max-width 300}
           [:input {:id          :new-todo
                    :value       (or new-todo "")
                    :placeholder "What needs to be done?"
                    :on-key-down (fn [e]
                                   (when (= (.-keyCode e) 13)
                                     (transact! [:todo/new! {:db/id     (random-uuid)
                                                             :todo/text new-todo}])
                                     (update-state! dissoc :new-todo)
                                     (transact! [:ui/set-ui! {:anim-type "char-in" :anim-delay 6}])))
                    :on-change   (fn [e]
                                   (update-state! assoc :new-todo (.-value (.-target e))))}]
           (when (seq todos)
             [:card [:list (for [todo todos]
                             [TodoItem todo])]])]))

(defcomponent New
  (query [[:ui/anim-type] [:ui/anim-delay]])
  (render [{:keys [:ui/anim-type :ui/anim-delay] :as res} state]
          [:div (make-text anim-type anim-delay)]))

(defcomponent Main
  (query [(ql/get-query TodoList)
          (ql/get-query New)])
  (render [res state]
          [:div
           ;; [New res]
           [TodoList res]]))

(defn remote-handler [query callback]
  (go (let [{:keys [status body] :as result} (<! (post "endpoint" {:edn-params query}))]
        (if (not= status 200)
          (print "server error: " body)
          (callback (read-string body))))))

(ql/mount {:component      Main
           :dom-element    (getElement "app")
           :state          app-state
           :remote-handler remote-handler
           :parsers        {:read   read
                            :mutate mutate
                            :remote remote
                            :sync   sync}})
