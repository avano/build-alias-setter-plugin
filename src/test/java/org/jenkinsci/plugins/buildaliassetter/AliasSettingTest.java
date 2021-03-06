package org.jenkinsci.plugins.buildaliassetter;

import hudson.matrix.MatrixBuild;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import hudson.tasks.BuildWrapper.Environment;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.LinkedHashSet;

import org.jenkinsci.plugins.buildaliassetter.BuildAliasSetter.DanglingAliasDeleter;
import org.jenkinsci.plugins.buildaliassetter.util.DummyProvider;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.util.reflection.Whitebox;

public class AliasSettingTest {

    @Mock private MatrixBuild build;
    @Mock private AbstractProject<?, ?> project;
    @Mock private BuildListener listener;
    @Mock private PermalinkStorage storage;
    @Mock private BuildAliasSetter.DescriptorImpl descriptor;

    private final ByteArrayOutputStream logBuffer = new ByteArrayOutputStream();
    private final PrintStream logger = new PrintStream(logBuffer);

    @Before
    public void setUp() {

        MockitoAnnotations.initMocks(this);

        Mockito.doReturn(42).when(build).getNumber();
        Mockito.doReturn(project).when(build).getProject();
        Mockito.doReturn(storage).when(project).getProperty(PermalinkStorage.class);

        Mockito.doReturn(logger).when(listener).getLogger();
    }

    @Test
    public void deleteAliasesUponBuildDeletion() throws IOException {

        final DanglingAliasDeleter deleter = new BuildAliasSetter.DanglingAliasDeleter();

        deleter.onDeleted(build);

        Mockito.verify(storage).deleteAliases(build);
        Mockito.verify(project).save();

        Mockito.verifyNoMoreInteractions(storage);
    }

    @Test
    public void setUpAndTearDownShouldAddAliases() throws Exception {

        BuildAliasSetter setter = DummyProvider.buildWrapper(
                null, "valid-alias", "42", "lastUnsuccessfulBuild", "", "1.480.3-SNAPSHOT"
        );

        final Environment environment = setter.setUp(build, null, listener);

        thenAttached("valid-alias", "1.480.3-SNAPSHOT");

        // Simulate different aliases provided before and after the build
        setUp();
        Whitebox.setInternalState(setter, "providers", DummyProvider.describableList("1.480.3", "valid-alias"));

        environment.tearDown(build, listener);

        thenAttached("1.480.3", "valid-alias");
    }

    @Test
    public void matrixSetUpShouldAddAliases() throws Exception {

        final BuildAliasSetter setter = DummyProvider.buildWrapper(
                null, "valid-alias", "42", "lastUnsuccessfulBuild", "", "1.480.3-SNAPSHOT"
        );

        setter.createAggregator(build, null, listener).startBuild();

        thenAttached("valid-alias", "1.480.3-SNAPSHOT");
    }

    @Test
    public void matrixTearDownShouldAddAliases() throws Exception {

        final BuildAliasSetter setter = DummyProvider.buildWrapper(
                null, "valid-alias", "42", "lastUnsuccessfulBuild", "", "1.480.3-SNAPSHOT"
        );

        setter.createAggregator(build, null, listener).endBuild();

        thenAttached("valid-alias", "1.480.3-SNAPSHOT");
    }

    private void thenAttached(final String... aliases) throws IOException {

        Mockito.verify(storage).addAliases(build, new LinkedHashSet<String>(Arrays.asList(aliases)));
        Mockito.verify(project).save();

        Mockito.verifyNoMoreInteractions(storage);
    }
}
