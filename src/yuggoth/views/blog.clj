(ns yuggoth.views.blog
  (:use noir.core hiccup.form)
  (:require markdown
            [yuggoth.views.util :as util]
            [noir.session :as session]
            [noir.response :as resp]
            [yuggoth.views.common :as common]
            [yuggoth.models.db :as db]
            [yuggoth.views.comments :as comments]))

(defn admin-forms [post-id]
  (when (session/get :admin) 
    [:div
     [:div (form-to [:post "/delete-post"]
                    (hidden-field "post-id" post-id)
                    [:span.delete "delete"])]
     [:div (form-to [:post "/update-post"]
                    (hidden-field "post-id" post-id)
                    [:span.submit "edit"])]]))

#_ (defn post-nav [id]
  (link-to (str "/blog/" (dec id)) "previous")
  (link-to (str "/blog/" (inc id)) "next"))

(defn entry [{:keys [id time title content author]}]
  (apply common/layout
         (if id
           [{:title title :elements (admin-forms id)}
            [:p#post-time (util/format-time time)]
            (markdown/md-to-html-string content)            
            (comments/get-comments id)            
            (comments/make-comment id)]
           ["Welcome to your new blog" "Nothing here yet..."])))

(defpage "/" []
  (if (db/get-admin)
    (util/cache :home (entry (db/get-last-post)))
    (resp/redirect "/create-admin")))

(defpage "/blog/:postid" {:keys [postid]}
  (util/cache (keyword (str "post-" postid)) (entry (db/get-post postid))))

(util/private-page [:post "/update-post"] {:keys [post-id]}
  (let [{:keys [title content]} (db/get-post post-id)] 
    (common/layout
      "Edit post"
      (form-to [:post "/make-post"]
               (text-field {:tabindex 1} "title" title)
               [:br]
               (text-area {:tabindex 2} "content" content)
               [:br]
               (hidden-field "post-id" post-id)
               [:span.submit {:tabindex 3} "post"]))))

(util/private-page "/make-post" {:keys [content error]}
  (common/layout
    "New post"
    (when error [:div.error error])
    [:div#output]
    (form-to [:post "/make-post"]
             (text-field {:placeholder "Title"} "title")
             [:br]
             (text-area {:onkeypress "render()"} "content" content)
             [:br]  
             [:span.submit {:tabindex 1} "post"])))

(util/private-page [:post "/make-post"] post
  (if (not-empty (:title post)) 
    (let [{:keys [post-id title content]} post]
      (if post-id
        (db/update-post post-id title content)
        (db/store-post title content (:handle (session/get :admin))))
      (resp/redirect (if post-id (str "/blog/" post-id) "/")))
    (render "/make-post" (assoc post :error "post title is required"))))

(util/private-page [:post "/delete-post"] {:keys [post-id]}
  (db/delete-post post-id)
  (resp/redirect "/"))
