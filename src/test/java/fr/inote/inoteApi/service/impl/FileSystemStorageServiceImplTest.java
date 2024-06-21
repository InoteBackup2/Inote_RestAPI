package fr.inote.inoteApi.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ActiveProfiles;

import fr.inote.inoteApi.crossCutting.exceptions.InoteFileNotFoundException;

/**
 * Unit tests of service UserService
 *
 * @author atsuhiko Mochizuki
 * @date 28/03/2024
 */

/*
 * The @ActiveProfiles annotation in Spring is used to declare which active bean
 * definition profiles
 * should be used when loading an ApplicationContext for test classes
 */
@ActiveProfiles("test")

/* Add Mockito functionalities to Junit 5 */
@ExtendWith(MockitoExtension.class)
public class FileSystemStorageServiceImplTest {
    /* DEPENDENCIES MOCKING */
    /* ============================================================ */
    /* @Mock create and inject mocked instances of classes */
    // @Mock

    /* DEPENDENCIES INJECTION */
    /* ============================================================ */
    /*
     * @InjectMocks instance class to be tested and automatically inject mock fields
     * Nota : if service is an interface, instanciate implementation withs mocks in
     * params
     */
    private final String STORAGE_TEST_LOCATION = "./storageTest";
    private final String STORAGE_LOCATION = "./storage/";

    private final String USER_IMG = "img/user.svg";

    @InjectMocks
    private FileSystemStorageService fileSystemStorageService_forTestingCreation = new FileSystemStorageService(
            STORAGE_TEST_LOCATION);

    @InjectMocks
    private FileSystemStorageService fileSystemStorageService = new FileSystemStorageService(STORAGE_LOCATION);

    /* REFERENCES FOR MOCKING */
    /* ============================================================ */

    /* FIXTURES */
    /* ============================================================ */

    /* SERVICE UNIT TESTS */
    /* ============================================================ */

    @Test
    @DisplayName("Init should create root directories")
    void init_shouldCreateRootDirectory() {
        if (Files.exists(Paths.get(STORAGE_TEST_LOCATION)))
            assertThatCode(() -> Files.delete(Path.of(STORAGE_TEST_LOCATION))).doesNotThrowAnyException();
        File folder = new File(STORAGE_TEST_LOCATION);
        assertThat(folder).doesNotExist();
        assertThatCode(() -> this.fileSystemStorageService_forTestingCreation.init()).doesNotThrowAnyException();
        folder = new File(STORAGE_TEST_LOCATION);
        assertThat(folder).exists();
        assertThatCode(() -> Files.delete(Path.of(STORAGE_TEST_LOCATION))).doesNotThrowAnyException();

    }

    @Test
    @DisplayName("load an existing path")
    void load_shouldSuccess_whenPathExists() {
        assertThatCode(() -> this.fileSystemStorageService_forTestingCreation.load(STORAGE_TEST_LOCATION))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Load as resource an existing file")
    void loadAsResource_ShouldSuccess_whenFileExists() throws InoteFileNotFoundException, IOException {
        Resource resource = null;
        resource = this.fileSystemStorageService.loadAsResource(USER_IMG);
        assertThat(resource).isNotNull();
        assertThat(resource.isFile()).isTrue();
        assertThat(resource.isReadable()).isTrue();
        assertThat(resource.getFilename()).isEqualTo("user.svg");
    }

    @Test
    @DisplayName("Load as resource an non-existing file")
    void loadAsResource_ShouldFail_whenFileNotExists() throws InoteFileNotFoundException, IOException {
        assertThatThrownBy(()->this.fileSystemStorageService.loadAsResource("iDontExist.png")).isInstanceOf(InoteFileNotFoundException.class);
    }

    

}
