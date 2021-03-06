(ns yuggoth.views.profile
  (:use hiccup.form hiccup.element noir.core clojure.pprint)
  (:require [yuggoth.views.common :as common]
            [yuggoth.views.util :as util]
            [noir.util.crypt :as crypt]
            [noir.session :as session]
            [yuggoth.models.db :as db]
            [noir.response :as resp]))

(defpage "/about" []
  (let [{:keys [about style handle email]} (db/get-admin)]   
    (common/layout
      (str "About " handle)            
      (markdown/md-to-html-string (str about))
      [:p [:b email]])))

(util/private-page "/profile" {:keys [title handle style email about pass pass1 pass2 info]}
  (common/layout
    "Profile"
    [:h2.info info]
    (link-to "/export" "export blog")
    
    #_ (form-to [:post "/import"]
                (text-area "blog")
                [:br]
                [:span.submit "import blog"])
    
    (form-to [:post "/update-tags"]
             (label "tags" "select tags to delete ") 
             (mapcat (fn [tag]
                       [(hidden-field (str "tag-" tag))
                        [:span.tagoff tag]])
                     (db/tags))             
             (submit-button "update tags"))
    
    (let [admin (session/get :admin)]            
      (form-to [:post "/profile"]
               (util/make-form "handle" "name" (or handle (:handle admin))
                               "style" "custom css url" (or style (:style admin))
                               "email" "email" (or email (:email admin))                               
                               "pass"  "password" nil
                               "pass1" "new password" nil
                               "pass2" "confirm password" nil)
               
               [:h2 "About"]                             
               (text-area {:id "content" :tabindex 6} "about" (or about (:about admin)))
               [:br]
               [:span.submit {:tabindex 7} "update profile"]))))

(defn get-updated-fields [profile] 
  (let [pass (:pass1 profile)
        updated-profile (select-keys profile [:title :handle :email :style :about])] 
    (if (not-empty pass) (assoc updated-profile :pass (when  (crypt/encrypt pass))) updated-profile)))

(defn update-profile [admin profile]  
  (let [updated-admin (merge admin (get-updated-fields profile))]    
    (try
      (session/remove! :admin)
      (session/put! :admin updated-admin)
      (db/update-admin updated-admin)      
      "profile updated successfully"
      (catch Exception ex (.getMessage ex)))))

(util/private-page [:post "/profile"] profile
  (let [{:keys [pass pass1 pass2]} profile        
        admin (session/get :admin)]     
    (render "/profile"
            (assoc profile 
                   :info
                   (cond (not (crypt/compare pass (:pass admin))) "wrong password"
                         (not= pass1 pass2) "passwords do not match"
                         :else (update-profile admin profile))))))

(util/private-page "/export" []
  (resp/content-type 
    "text/plain" 
    (let [buf (new java.io.StringWriter)] 
      (pprint (db/export) buf)
      (.toString buf))))

(defpage [:post "/import"] {:keys [blog]}
  (db/import-posts blog)
  (resp/redirect "/profile"))

(defpage [:post "/update-tags"] tags
  (db/delete-tags (map second tags))
  (resp/redirect "/profile"))

