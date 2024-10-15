package org.file.grain.enrichment.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.StandardIntegrationFlow;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.FileWritingMessageHandler;
import org.springframework.messaging.MessageChannel;

@Configuration
public class FileIntegrationConfig {
	
	 private static final String INPUT_DIR = "D:\\workspace_support\\input";    // Répertoire source
	    private static final String PROCESSED_DIR = "D:\\workspace_support\\processed"; // Répertoire destination "traités"

	    @Bean
	    public MessageChannel fileInputChannel() {
	        return new DirectChannel();
	    }

	    // Source des fichiers à lire (dossier 'input')
	    @Bean
	    public FileReadingMessageSource fileReadingMessageSource() {
	        FileReadingMessageSource source = new FileReadingMessageSource();
	        source.setDirectory(new File(INPUT_DIR));
	        return source;
	    }

	    // Déplacement des fichiers traités vers le dossier 'processed'
	    @Bean
	    @ServiceActivator(inputChannel = "fileOutputChannel")
	    public FileWritingMessageHandler fileWritingMessageHandler() {
	        FileWritingMessageHandler handler = new FileWritingMessageHandler(new File(PROCESSED_DIR));
	        handler.setAutoCreateDirectory(true);  // Crée automatiquement le dossier si nécessaire
	        handler.setExpectReply(false);
	        return handler;
	    }

	 // Flux d'intégration : lire, convertir en majuscule, écrire
	    @Bean
	    public StandardIntegrationFlow fileIntegrationFlow() {
	        return IntegrationFlows.from(fileReadingMessageSource(), spec -> spec.poller(p -> p.fixedDelay(5000)))
	                .channel(fileInputChannel()) // Lire les fichiers du channel
	                .handle((payload, headers) -> {
	                    // Traitement : convertir le contenu du fichier en majuscules
	                    File file = (File) payload;
	                    try {
	                        Path path = file.toPath();
	                        String content = Files.lines(path)
	                        		         .map(String::toUpperCase)
	                        		         .collect(Collectors.joining("\n"));
	                        

	                        // Écrire le contenu en majuscules dans le fichier original
	                        Files.write(path, content.getBytes());

	                    } catch (IOException e) {
	                        e.printStackTrace();
	                    }
	                    return file; // Renvoie le fichier traité
	                })
	                .channel("fileOutputChannel") // Envoyer vers le channel de sortie
	                .get();
	    }

}
