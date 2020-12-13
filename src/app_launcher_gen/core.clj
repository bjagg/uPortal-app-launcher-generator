(ns app-launcher-gen.core
  (:gen-class)
  (:use seesaw.core
        seesaw.table
        [clojure.pprint :only [pprint]]
        [clojure.java.io :only [as-file make-parents]]))

; Tester for actions -- display a simple alert to confirm working action listener
(defn works? [e]
  (alert e "Action works!"))

; Data file

(def data-filename "data.clj")

(defn save-entries!
  "Save entries to data.clj"
  [data]
  (spit data-filename (with-out-str (pprint data))))

(defn load-entries!
  "Load entries from data.clj"
  []
  (if (.exists (as-file data-filename))
    (read-string (slurp data-filename))))

; Template file

(def template-filename "portlet-definition.xml")

(defn read-template!
  "Read in template as a string"
  []
  (slurp template-filename))

(def template (read-template!))

(defn template->def
  "Replace tokens in template with data, return as string"
  [row]
  (-> template
      (clojure.string/replace #"\{\{fname\}\}" (:fname row))
      (clojure.string/replace #"\{\{title\}\}" (:title row))
      (clojure.string/replace #"\{\{desc\}\}" (:desc row))
      (clojure.string/replace #"\{\{cat\}\}" (:cat row))
      (clojure.string/replace #"\{\{group\}\}" (:group row))
      (clojure.string/replace #"\{\{app-url\}\}" (:app-url row))
      (clojure.string/replace #"\{\{icon-url\}\}" (:icon-url row))))

(defn generate-def-file!
  "Create portlet definition files from row of data"
  [row]
  (let [out (template->def row)
        filename (str "portlet-definition/" (:fname row) ".portlet-definition.xml")]
    (make-parents filename)
    (spit filename out)))

; Entry

(def entry-model (table-model
                     :columns [
                                {:key :fname :text "fname"}
                                {:key :title :text "Title"}
                                {:key :desc :text "Description"}
                                {:key :cat :text "Category"}
                                {:key :group :text "User Group"}
                                {:key :app-url :text "Application URL"}
                                {:key :icon-url :text "Icon URL"}
                              ]
                      :rows [["banner-app" "Banner App" "My banner app" "Registrar" "Students" "https://banner.school.edu" "banner_32x32.png"]
                             ["another-app" "Another App" "my second app" "News" "Everyone" "https://another.school.edu" "another-app_32x32.png"]]))

(update-proxy entry-model {"isCellEditable" (fn [this row col] true)})

(defn set-entries!
  "Clear and set entry model with data. TODO: check data format."
  []
  (if-let [data (load-entries!)]
    (do
      #_(pprint (load-entries!))
      (clear! entry-model)
      (apply insert-at! entry-model (interleave (iterate identity 0) data)))))

(def entry-table (table
                   :id :entry-table
                   :size [600 :by 600]
                   :show-grid? true
                   :model entry-model))

; Add new row button

(def btn-add-row (button :text "New Entry"))

(defn add-new-entry
  "Add new row to entry-model"
  [e]
  (let [n (row-count entry-model)]
    (insert-at! entry-model n ["new-fname" "Title" "Description" "News" "Everyone" "https://" "new-fname_32x32.png"])))

(listen btn-add-row :action add-new-entry)

; Generate button

(def btn-generate (button :text "Generate"))

(defn generate-files!
  "Generate files based on curren table values."
  [e]
  (let [n (row-count entry-model)
        data (value-at entry-model (range n))]
    (save-entries! data)
    (pprint (template->def (first data)))
    (generate-def-file! (first data))
    (doall (map generate-def-file! data))
    ))

(listen btn-generate :action generate-files!)

; Main window

(def panel (border-panel
             :center (scrollable entry-table)
             :south (border-panel :west btn-add-row :east btn-generate)
             :vgap 5 :hgap 5 :border 7))

(def main-frame (frame
                  :title "uPortal App Launcher Defs"
                  :content panel
                  :on-close :exit))

(defn foo
  "I don't do a whole lot."
  [x]
  (println x "Hello, World!"))


(defn -main [& args]
  (invoke-later
    (-> main-frame
        pack!
        show!))
  (set-entries!))
