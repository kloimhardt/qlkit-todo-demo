(ns qlkit-todo.parsers-db
  (:require [qlkit.core :refer [parse-children parse-children-remote parse-children-sync] :as ql]
            [goog.string :as gstring]
            [goog.string.format]))


(defn conn [] (get @(get (deref ql/mount-info) :state) :conn))

(defn quote-string [text] (str "\"" text "\""))

(defn query [txt]
  (js->clj (.exec (conn) txt) :keywordize-keys true))

(defn table-set [conn]
  (let [q "SELECT name FROM sqlite_master
    WHERE type IN (\"table\",\"view\")
    AND name NOT LIKE \"sqlite_%\" ORDER BY 1"]
    (into #{} (first (:values (first (.exec conn q)))))))

(def format gstring/format)

(defn create-tables-if-necessary [conn] ;; klm TODO use [IF NOT EXISTS]
  (let [table-set (table-set conn)
        new-table-name "todos"
        qry (format
              "CREATE TABLE %s
               (id INTEGER PRIMARY KEY,
                text TEXT NOT NULL)"
              new-table-name)]
    (when-not (contains? table-set new-table-name)
      (print (str "Creating new table " new-table-name))
      (.exec conn qry))))

(defmulti read (fn [qterm & _] (first qterm)))

(defn fvf [x]
  (first (:values (first x))))

(defn db-get-ids []
  (fvf (query "select id from todos")))

(defmethod read :qlkit-todo/todos
  [[dispatch-key params :as query-term] env {:keys [:todo/by-id] :as state}]
  (println "read :qlkit-todo/todos" (db-get-ids))
  (let [{:keys [todo-id]} params]
    (if todo-id
      [(parse-children query-term (assoc env :todo-id todo-id))]
      (for [id (keys by-id)]
        (parse-children query-term (assoc env :todo-id id))))))

(defn db-get-text [id]
  (first (fvf (query (str "select text from todos where id=" id)))))

(defmethod read :todo/text
  [query-term {:keys [todo-id] :as env} state]
  ;;(println "read :todo/text" (db-get-text todo-id))
  (get-in state [:todo/by-id todo-id :todo/text]))

(defn db-get-id [id]
  (first (fvf (query (str "select id from todos where id=" id)))))

(defmethod read :db/id
  [query-term {:keys [todo-id] :as env} state]
  ;;(println "read :db/id" (db-get-id todo-id))
  (when (get-in state [:todo/by-id todo-id])
      todo-id))

(defmulti mutate (fn [qterm & _] (first qterm)))

(defn db-insert-text [text]
  (query (format "INSERT INTO todos (text) VALUES (%s)"
                 (quote-string text)))
  (let [local-id (first (fvf (query "SELECT last_insert_rowid()")))]
    {:desc "insert" :local-id local-id :txt (db-get-text local-id)}))

(defmethod mutate :todo/new!
  [[dispatch-key params :as query-term] env state-atom]
  (let [{:keys [:db/id :todo/text]} params]
    ;;(println "mutate :todo/new!" (db-insert-text text))
    (swap! state-atom assoc-in [:todo/by-id id] params)))

(defmethod mutate :todo/delete!
  [query-term {:keys [todo-id] :as env} state-atom]
  (println "mutate :todo/delete" @state-atom)
  (swap! state-atom update :todo/by-id dissoc todo-id))

(defmulti remote (fn [qterm & _] (first qterm)))

(defmethod remote :todo/new!
  [query-term state env]
  (println "remote :todo/new" state)
  query-term)

(defmethod remote :todo/delete!
  [query-term state env]
  (println "remote :todo/delete!" state)
  query-term)

(defmethod remote :todo/text
  [query-term state env]
  (println "remote :todo/text" state)
  query-term)

(defmethod remote :db/id
  [query-term state env]
  (println "remote :db/id" state)
  query-term)

(defmethod remote :qlkit-todo/todos
  [query-term state env]
  (println "remote :qlkit-todo/todos" state)
  (parse-children-remote query-term env))

(defmulti sync (fn [qterm & _] (first qterm)))

(defmethod sync :qlkit-todo/todos
  [[_ params :as query-term] result env state-atom]
  (println "sync :qlkit-todo/todos" state-atom)
  (for [{:keys [db/id] :as todo} result]
    (parse-children-sync query-term todo (assoc env :db/id id))))

(defmethod sync :todo/text
  [query-term result {:keys [:db/id] :as env} state-atom]
  (println "sync :todo/text" state-atom)
  (when id
    (swap! state-atom assoc-in [:todo/by-id id :todo/text] result)))

(defmethod sync :db/id
  [query-term result {:keys [:db/id] :as env} state-atom]
  (println "sync :db/id" state-atom)
  (when id
    (swap! state-atom assoc-in [:todo/by-id id :db/id] result)))

(defmethod sync :todo/new!
  [query-term result env state-atom]
  (println "sync :todo/new!" state-atom)
  (let [[temp-id permanent-id] result]
    (swap! state-atom
           update
           :todo/by-id
           (fn [by-id]
             (-> by-id
                 (dissoc temp-id)
                 (assoc permanent-id (assoc (by-id temp-id) :db/id permanent-id)))))))
