package com.fitmap.gateway.config;

import java.io.IOException;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FirebaseAuthConfig {

    @Bean
    public FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {

        return FirebaseAuth.getInstance(firebaseApp);
    }

    @Bean
    public FirebaseApp firebaseApp(FirebaseOptions firebaseOptions) {

        return FirebaseApp.initializeApp(firebaseOptions);
    }

    @Bean
    public FirebaseOptions firebaseOptions(GoogleCredentials googleCredentials) {

        return FirebaseOptions.builder().setCredentials(googleCredentials).build();
    }

    @Bean
    public GoogleCredentials googleCredentials() throws IOException {

        return GoogleCredentials.getApplicationDefault();
    }

}
