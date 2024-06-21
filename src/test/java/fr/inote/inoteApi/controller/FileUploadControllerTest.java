package fr.inote.inoteApi.controller;

import static fr.inote.inoteApi.ConstantsForTests.REFERENCE_USER_NAME;
import static fr.inote.inoteApi.ConstantsForTests.REFERENCE_USER_PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.inote.inoteApi.crossCutting.constants.Endpoint;
import fr.inote.inoteApi.crossCutting.enums.RoleEnum;
import fr.inote.inoteApi.crossCutting.security.impl.JwtServiceImpl;
import fr.inote.inoteApi.dto.ProtectedUserResponseDto;
import fr.inote.inoteApi.dto.PublicUserResponseDto;
import fr.inote.inoteApi.dto.UserRequestDto;
import fr.inote.inoteApi.entity.Role;
import fr.inote.inoteApi.entity.User;
import fr.inote.inoteApi.service.impl.FileSystemStorageService;
import fr.inote.inoteApi.service.impl.UserServiceImpl;

/**
 * Unit tests of FileUploadControllerTest
 *
 * @author atsuhikoMochizuki
 * @date 2024-06-21
 */
@WebMvcTest(FileUploadController.class)
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
public class FileUploadControllerTest {

     /* DEPENDENCIES INJECTION */
        /* ============================================================ */

        @Autowired
        private MockMvc mockMvc;
        @Autowired
        private ObjectMapper objectMapper;


        @MockBean
        private FileSystemStorageService fileSystemStorageService;

        @MockBean
        private AuthenticationManager authenticationManager;
        @MockBean
        private JwtServiceImpl jwtServiceImpl;
        @MockBean
        private UserServiceImpl userService;

        /* REFERENCES FOR MOCKING */
        /* ============================================================ */
        
        private Resource resourceRef;
       

        /* FIXTURES */
        /* ============================================================ */
        @BeforeEach
        void init() throws MalformedURLException {
            this.resourceRef =  new UrlResource(Paths.get("./storage").resolve("avatars/user.svg").toUri());
            
            System.out.println("toto");
        }

        /* CONTROLLER UNIT TEST */
        /* ============================================================ */
        
        @Test
        @DisplayName("Serve an existing file")
        public void serveFile_shouldSuccess_whenFileExists() throws Exception{
            /* Arrange */
            when(this.fileSystemStorageService.loadAsResource(anyString())).thenReturn(this.resourceRef);
        
            /* Act & assert */
            this.mockMvc.perform(get(String.format("/api/files/avatars/user.svg")))
            .andExpect(MockMvcResultMatchers.status().isOk());
        }

}
