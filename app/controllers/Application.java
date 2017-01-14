package controllers;

import models.User;
import model.*;
import models.UserFacebook;
import play.Logger;
import play.libs.OAuth2;
import play.libs.WS;
import play.mvc.Before;
import play.mvc.Controller;
import java.util.*;
import java.io.*;
import java.IOException.*;
import java.net.URLEncoder;
import org.json.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import java.net.*;

public class Application extends Controller 
{
    /*
    *
    * Permet d'accéder et d'utiliser l'API Facebook
    * Les deux clés sont les clés fournies par facebook au moment de la création de l'app
    *
    */
    public static OAuth2 FACEBOOK = new OAuth2(
        "https://graph.facebook.com/oauth/authorize",
        "https://graph.facebook.com/oauth/access_token",
        "1630145123955332",
        "0a74e2888d6c3e0c31a40f7130be5b84"
    );

    public static void index() 
    {
        render();
    }

    /**
    *
    *  Cette méthode est appellée lorsque l'on arrive sur l'URL de type xxx.com/map
    *  Elle permet d'afficher sur une carte les contacts indéxés via Elastic search 
    *
    */
    public static void map()
    {
        JsonObject friends = WS.url("http://localhost:9200/users/_search?pretty=true").get().getJson().getAsJsonObject();
        render(friends);
    }

    /**
    *
    *  Cette méthode est appellée lorsque l'on arrive sur l'URL de type xxx.com/search
    *  Permet de renvoyer les résultats qui ont matché avec la recherche de l'utilisateur  
    *
    */
    public static void search()
    {

        String fullName = params.get("fullName");
        String userName = params.get("userName");

        // l'utilisateur arrive simplement sur la page search... On affiche simplement la page.
        if (fullName == null && userName == null)
            render();

        // un des deux n'est pas nul donc on a fait une recherche... il faut maintenant faire une requête sur elastic search
        else
        {
            // plusieurs possibilités ici... Les deux champs sont remplis, ou seulement un des deux est rempli !
            // C'est interessant ici pour faire la recherche sur l'index qui a été rempli sur la page html

            JsonObject fullNameResult = null; // jsonobject pour la recherche sur le fullname
            JsonObject userNameResult = null; // json object pour la recherche sur l'username

            // On a le fullName mais aucun userName, on requête sur le champ fullname
            if (!fullName.equals("") && userName.equals(""))
            {
                String request = "http://localhost:9200/users/_search?q=name:*"+fullName+"*";
                fullNameResult = WS.url(request).get().getJson().getAsJsonObject();
                System.out.print(fullNameResult);
                render(fullNameResult, userNameResult);
            }

            // on a le userName mais pas le fullName, on va requêter sur le champ userName
            else if (fullName.equals("") && !userName.equals(""))
            {
                String request = "http://localhost:9200/users/_search?q=username:*"+userName+"*";
                userNameResult = WS.url(request).get().getJson().getAsJsonObject();
                System.out.print(userNameResult);
                render(fullNameResult,userNameResult);
            }
            
            else
            {
                String request = "http://localhost:9200/users/_search?q=name:*"+fullName+"*";
                fullNameResult = WS.url(request).get().getJson().getAsJsonObject();
                String request2 = "http://localhost:9200/users/_search?q=username:*"+userName+"*";
                userNameResult = WS.url(request2).get().getJson().getAsJsonObject();

                render(fullNameResult,userNameResult);
            } 
        }
    }

    /**
    *
    *  Affichage de la page more.html
    *  
    *
    */
    public static void more()
    {
        render();
    }

    /**
    *
    *  Cette méthode est appellée lorsque l'on arrive sur l'URL de type xxx.com/instagram
    *  Permet de récupérer les informations liées à Instagram  
    *
    */
    public static void instagram()
    {
        // VARIABLES GLOBALES : ce sont des constantes qui ne peuvent pas être modifiées
        final String ACCESS_TOKEN = "https://api.instagram.com/oauth/access_token";
        final String CLIENT_ID = "f3d30e62a988444e9c53b26e05b0e5cf";
        final String CLIENT_SECRET = "e507713098834267b4eaee4e0bffb615";
        final String REDIRECT_URI = "http://projetmobilite.com:9001/instagram";
        final String GRANT_TYPE = "authorization_code";
        final String CODE = params.get("code"); // On récupère le code qui sera échangé contre l'access token

        // On récupèrera les données au format JSON. D'une part les utilisateurs que l'utilisateur follow, d'autre part les infos sur l'utilisateur connecté.
        JsonObject myFollow = null;
        JsonObject myData = null;
        String ACCESSTOKEN;
        String requeteMyFollow;
        String requeteMyData;
        
        // On envoi la requête de façon à récupérer l'access token. Les paramètres permettent de nous identifier facilement.
        WS.HttpResponse responseToken = WS.url(ACCESS_TOKEN)
            .setParameter("code", CODE)
            .setParameter("client_id", CLIENT_ID )
            .setParameter("client_secret", CLIENT_SECRET)
            .setParameter("redirect_uri", REDIRECT_URI)
            .setParameter("grant_type", GRANT_TYPE)
            .post() ;
        
        // On a récupérer la variable CODE, on peut donc procéder au traitement.
        if (CODE != null)
        {
            JsonElement jsonEltToken = responseToken.getJson() ; // on récupère la réponse au format JSON
            JsonObject jsonObjectToken = jsonEltToken.getAsJsonObject() ; // Convertion du JSonElement en JSonObject
            ACCESSTOKEN = jsonObjectToken.get("access_token").toString() ;
            ACCESSTOKEN = ACCESSTOKEN.replace('"', ' '); // on enlève les guillemets autour de l'access token

            /*********************************************************************************************
            **********************************************************************************************
                    A cette étape, l'utilisateur est correctement identifié et nous pouvons procédé
                    à la récupération de ces informations. Dans un premier temps, on récupère les
                        informations qui le concernent, puis on récupère les amis qu'il follow. 
            **********************************************************************************************
            *********************************************************************************************/

            // Récupération des follows de l'utilisateur connecté
            requeteMyFollow = "https://api.instagram.com/v1/users/self/followed-by?access_token="+ACCESSTOKEN;
            WS.HttpResponse responseMyFollow = WS.url(requeteMyFollow).get();
            JsonElement jsonEltMyFollow = responseMyFollow.getJson();
            myFollow = jsonEltMyFollow.getAsJsonObject();  

            // Récupération des informations de l'utilisateur connecté
            requeteMyData = "https://api.instagram.com/v1/users/self/?access_token="+ACCESSTOKEN;
            WS.HttpResponse responseMyData = WS.url(requeteMyData).get();
            JsonElement jsonEltMyData = responseMyData.getJson();
            myData = jsonEltMyData.getAsJsonObject(); 


            if (myFollow != null) {
                JsonArray datas = myFollow.getAsJsonArray("data");

                ArrayList<ArrayList<String>> infos_instagram;
                infos_instagram = new ArrayList<ArrayList<String>>();
                for (int i = 0; i < datas.size(); i++) {
                    ArrayList<String> informations = new ArrayList<String>();

                    JsonElement info = datas.get(i);

                    JsonObject info_object = info.getAsJsonObject();  

                    informations.add(info_object.get("profile_picture").getAsString());
                    informations.add(info_object.get("full_name").getAsString());
                    informations.add(info_object.get("username").getAsString());

                    infos_instagram.add(informations);
                }

                String json_content = "";
                int identifiant = 1000;
                // CONSTRUCTION DU CONTENU JSON
                for(int k = 0; k < infos_instagram.size(); k++){

                String url_picture_value = null;
                String name_value        = null;
                String username_value    = null;

                // PARCOURS POUR RECUPERER LES INFOS DU CONTACT
                for(int n = 0; n < infos_instagram.get(k).size(); n++){

                    if (n == 0) {
                        url_picture_value = infos_instagram.get(k).get(n);
                    }
                    else if (n == 1) {
                        name_value = infos_instagram.get(k).get(n);
                    }
                    else if (n == 2) {
                     username_value = infos_instagram.get(k).get(n);   
                    }

                }

                    float minX = 43.0f;
                    float maxX = 49.0f;

                    Random rand = new Random();
                    float latitude = rand.nextFloat() * (maxX - minX) + minX;

                    float minLongitude = 0.0f;
                    float maxLongitude = 6.0f;

                    Random rand2 = new Random();
                    float longitude = rand.nextFloat() * (maxLongitude - minLongitude) + minLongitude;

                    json_content = json_content + "{\"index\":{\"_index\":\"users\",\"_type\":\"instagram\",\"_id\":" + identifiant + "}}\n";
                    json_content = json_content + "{\"line_id\": " + identifiant + ", \"name\": \""+ name_value + "\", \"username\": \""+username_value +"\", \"picture\": \""+url_picture_value+"\", \"longitude\": \"" + longitude + "\" , \"latitude\" :\"" + latitude + "\" }\n";
                    json_content = json_content + "\n";
                    identifiant++;
                }
                
                try {
                    // INSERTION DES CONTACTS DANS NOTRE FICHIER JSON
                    FileWriter fichier = new FileWriter("data_instagram.json");
                    fichier.write (json_content);
                    fichier.close();
                } 
                catch (Throwable t)
                {
                    t.printStackTrace();
                }
            }

            // IMPORTATION DES CONTACTS DANS ELASTICSEARCH
            StringBuffer output = new StringBuffer();
            Process p;
            try {
                String[] cmd2 = { "./index_instagram.sh"};
                Process p2 = Runtime.getRuntime().exec(cmd2);
                p2.waitFor();

                BufferedReader reader = 
                new BufferedReader(new InputStreamReader(p2.getInputStream()));

                String line = "";           
                while ((line = reader.readLine())!= null) {
                    output.append(line + "\n");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            System.out.println(output.toString());

        }
        
        render(myData, myFollow); // on envoie les deux JSON a la page instagram.html qui se chargera de les afficher.
    }

    /**
    *
    *  Cette méthode est appellée lorsque l'on arrive sur l'URL de type xxx.com/facebook
    *  Permet de récupérer les informations liées à Facebook  
    *
    */
    public static void facebook()
    {
        UserFacebook u = connected();
        JsonObject me = null;
        JsonObject friends = null;
        JsonObject friends_number = null;

        if (u != null && u.access_token != null)
            me = WS.url("https://graph.facebook.com/v2.8/me/?access_token=%s", WS.encode(u.access_token)).get().getJson().getAsJsonObject();

        if (u != null && u.access_token != null)
            friends = WS.url("https://graph.facebook.com/v2.8/me/taggable_friends/?access_token=%s", WS.encode(u.access_token)).get().getJson().getAsJsonObject();

        if (u != null && u.access_token != null)
            friends_number = WS.url("https://graph.facebook.com/v2.8/me/friends/?access_token=%s", WS.encode(u.access_token)).get().getJson().getAsJsonObject();

        if (friends != null) {
            JsonArray datas = friends.getAsJsonArray("data");

            ArrayList<ArrayList<String>> infos_facebook;
            infos_facebook = new ArrayList<ArrayList<String>>();
            for (int i = 0; i < datas.size(); i++) {
                ArrayList<String> informations = new ArrayList<String>();
                JsonElement info = datas.get(i);
                JsonObject info_object = info.getAsJsonObject(); 

                // RECUPERATION DE L'URL DE LA PHOTO DE PROFIL
                JsonElement picture = info_object.get("picture");
                JsonObject info_picture = picture.getAsJsonObject();

                JsonElement url_element = info_picture.get("data");
                JsonObject url_info = url_element.getAsJsonObject();

                // On stocke les infos dans notre matrice
                informations.add(url_info.get("url").getAsString());
                informations.add(info_object.get("name").getAsString());

                infos_facebook.add(informations);
            }

            String json_content = "";
            // CONSTRUCTION DU CONTENU JSON
            for(int i = 0; i < infos_facebook.size(); i++){

                String url_picture_value = null;
                String name_value = null;

                // PARCOURS POUR RECUPERER LES INFOS DU CONTACT
                for(int m = 0; m < infos_facebook.get(i).size(); m++){

                    if (m == 0) {
                        url_picture_value = infos_facebook.get(i).get(m);
                    }
                    else if (m == 1) {
                        name_value = infos_facebook.get(i).get(m);
                    }

                }
                float minX = 43.0f;
                float maxX = 49.0f;

                Random rand = new Random();
                float latitude = rand.nextFloat() * (maxX - minX) + minX;

                float minLongitude = 0.0f;
                float maxLongitude = 6.0f;

                Random rand2 = new Random();
                float longitude = rand.nextFloat() * (maxLongitude - minLongitude) + minLongitude;
                json_content = json_content + "{\"index\":{\"_index\":\"users\",\"_type\":\"facebook\",\"_id\":" + i + "}}\n";
                json_content = json_content + "{\"line_id\": " + i + ", \"name\": \""+ name_value + "\", \"username\": \" \", \"picture\": \""+url_picture_value+"\", \"longitude\": \"" + longitude + "\" , \"latitude\" :\"" + latitude + "\" }\n";
                json_content = json_content + "\n";
            }

            try {
                // INSERTION DES CONTACTS DANS NOTRE FICHIER JSON
                FileWriter fichier = new FileWriter("data_facebook.json");
                fichier.write (json_content);
                fichier.close();
            } 
            catch (Throwable t)
            {
                t.printStackTrace();
            }
        }

        // IMPORTATION DES CONTACTS DANS ELASTICSEARCH
        StringBuffer output = new StringBuffer();
        Process p;
        try {
            String[] cmd2 = { "./index_facebook.sh"};
            Process p2 = Runtime.getRuntime().exec(cmd2);
            p2.waitFor();

            BufferedReader reader = new BufferedReader(new InputStreamReader(p2.getInputStream()));

            String line = "";           
            while ((line = reader.readLine())!= null) {
                output.append(line + "\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println(output.toString());

        render(me, friends, friends_number);
    }

    public static void auth() 
    {
        if (OAuth2.isCodeResponse()) 
        {
            UserFacebook u = connected();
            OAuth2.Response response = FACEBOOK.retrieveAccessToken(authURL());
            u.access_token = response.accessToken;
            u.save();
            index();
        }
        FACEBOOK.retrieveVerificationCode(authURL());
    }

    @Before
    static void setuser() 
    {
        UserFacebook user = null;
        
        if (session.contains("uid")) 
        {
            Logger.info("existing user: " + session.get("uid"));
            user = UserFacebook.get(Long.parseLong(session.get("uid")));
        }
        
        if (user == null) 
        {
            user = UserFacebook.createNew();
            session.put("uid", user.uid);
        }
        renderArgs.put("user", user);
    }

    static String authURL() 
    {
            return play.mvc.Router.getFullUrl("Application.auth");
    }

    static UserFacebook connected() 
    {
        return (UserFacebook)renderArgs.get("user");
    }
}
