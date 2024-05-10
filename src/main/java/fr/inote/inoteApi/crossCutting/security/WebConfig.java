package fr.inote.inoteApi.crossCutting.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import fr.inote.inoteApi.crossCutting.constants.Endpoint;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${inote.frontend.host}")
    private String FRONTEND_HOST;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping(Endpoint.REGISTER)
                //.allowedOrigins(FRONTEND_HOST)	// MUST BE rEPLACED!
                .allowedOrigins("*") // WARNING IT IS FOR DEV
                .allowedMethods("POST")
                // .allowedHeaders("Content-Type", "Authorization")
                .exposedHeaders("Access-Control-Allow-Origin", "Access-Control-Allow-Credentials");

                registry.addMapping(Endpoint.ACTIVATION)
                //.allowedOrigins(FRONTEND_HOST)
                .allowedOrigins("*")
                .allowedMethods("POST")
                // .allowedHeaders("Content-Type", "Authorization")
                .exposedHeaders("Access-Control-Allow-Origin", "Access-Control-Allow-Credentials");

                registry.addMapping(Endpoint.SIGN_IN)
                //.allowedOrigins(FRONTEND_HOST)
                .allowedOrigins("*")
                .allowedMethods("POST")
                // .allowedHeaders("Content-Type", "Authorization")
                .exposedHeaders("Access-Control-Allow-Origin", "Access-Control-Allow-Credentials");

    }
}

// @Configuration
// @EnableWebMvc
// public class WebConfig {

//     // Validity time of config
//     private static final Long MAX_AGE = 3600L;
//     private static final int CORS_FILTER_ORDER = -102;

//     /**
//      * CORS est une fonction de sécurité des navigateurs web qui empêche les pages
//      * web d'envoyer des requêtes à une origine (domaine, protocole ou port)
//      * différente de celle à partir de laquelle la page web a été chargée.
//      */
//     @SuppressWarnings("rawtypes")
//     @Bean
//     public FilterRegistrationBean corsFilter() {
//         /**
//          * UrlBasedCorsConfigurationSource est une source de configuration pour CORS
//          * (Cross-Origin Resource Sharing) dans Spring MVC. Elle est utilisée pour
//          * enregistrer des configurations CORS pour des modèles d'URL spécifiques. La
//          * méthode registerCorsConfiguration est utilisée pour enregistrer un objet
//          * CorsConfiguration pour un modèle de chemin spécifique.
//          * 
//          * La classe UrlBasedCorsConfigurationSource est responsable de la résolution du
//          * chemin de recherche à l'aide de l'UrlPathHelper et de l'enregistrement de
//          * l'objet CorsConfiguration pour le chemin résolu.
//          */
//         UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

//         /* Setup cors config */
//         CorsConfiguration config = new CorsConfiguration();

//         // Request whith authentication informations could be possible
//         config.setAllowCredentials(true);

//         // AllowedOrigin vous permet de spécifier quels domaines, schémas ou ports sont
//         // autorisés à adresser des requêtes à votre serveur.
//         config.addAllowedOrigin("http://localhost:4200"); 

//         /**
//          * La méthode setAllowedHeaders est utilisée pour spécifier la liste des
//          * en-têtes qu'une requête pré-vol peut répertorier comme étant autorisés à être
//          * utilisés lors d'une requête réelle. Cette méthode fait partie du protocole
//          * CORS (Cross-Origin Resource Sharing).
//          * 
//          * Dans le contexte de CORS, une requête préliminaire est une requête envoyée
//          * par le navigateur au serveur pour vérifier si le protocole CORS est compris
//          * et si le serveur connaît les méthodes et les en-têtes spécifiques que le
//          * navigateur va utiliser dans la requête réelle. La méthode setAllowedHeaders
//          * est utilisée pour spécifier quels en-têtes sont autorisés à être envoyés dans
//          * la demande réelle.
//          */
//         config.setAllowedHeaders(Arrays.asList(
//                 HttpHeaders.AUTHORIZATION,
//                 HttpHeaders.CONTENT_TYPE,
//                 HttpHeaders.ACCEPT));


//         config.setAllowedMethods(Arrays.asList(
//                 HttpMethod.GET.name(),
//                 HttpMethod.POST.name(),
//                 HttpMethod.PUT.name(),
//                 HttpMethod.DELETE.name()));

//         config.setMaxAge(MAX_AGE);

//         source.registerCorsConfiguration("/**", config);

//         /**
//          * L'objectif du FilterRegistrationBean est de personnaliser les propriétés
//          * d'enregistrement d'un filtre web dans une application basée sur Spring. Il
//          * vous permet de définir le modèle d'URL, les types de distributeurs et l'ordre
//          * du filtre, ce qui offre un meilleur contrôle sur le comportement du filtre
//          * par rapport à l'utilisation d'un bean Spring standard
//          */
//         @SuppressWarnings("unchecked")
//         FilterRegistrationBean bean = new FilterRegistrationBean(new CorsFilter(source));

        
//         /* En le plaçant à -100, on s'assure que le filtre Cors soit passé avant
//          * le SpringSecurityFilter
//          */
//         bean.setOrder(CORS_FILTER_ORDER);

//         return bean;
//     }
// }
