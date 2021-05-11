package repository;

import static org.assertj.core.api.Assertions.assertThat;

import io.ebean.DB;
import io.ebean.Transaction;
import models.LifecycleStage;
import models.Version;
import org.junit.Before;
import org.junit.Test;

public class VersionRepositoryTest extends WithPostgresContainer {
  private VersionRepository versionRepository;

  @Before
  public void setupProgramRepository() {
    versionRepository = instanceOf(VersionRepository.class);
  }

  @Test
  public void testPublish() {
    this.resourceCreator.insertActiveProgram("foo");
    this.resourceCreator.insertActiveProgram("bar");
    this.resourceCreator.insertDraftProgram("bar");
    assertThat(this.versionRepository.getActiveVersion().getPrograms()).hasSize(2);
    assertThat(this.versionRepository.getDraftVersion().getPrograms()).hasSize(1);
    Version oldDraft = this.versionRepository.getDraftVersion();
    this.versionRepository.publishNewSynchronizedVersion();
    assertThat(this.versionRepository.getActiveVersion().getPrograms()).hasSize(2);
    assertThat(this.versionRepository.getDraftVersion().getPrograms()).hasSize(0);
    oldDraft.refresh();
    assertThat(oldDraft.getLifecycleStage()).isEqualTo(LifecycleStage.ACTIVE);
  }

  @Test
  public void testSetLive() {
    this.resourceCreator.insertActiveProgram("foo");
    this.resourceCreator.insertDraftProgram("bar");
    Version oldDraft = this.versionRepository.getDraftVersion();
    Version oldActive = this.versionRepository.getActiveVersion();
    this.versionRepository.publishNewSynchronizedVersion();
    oldDraft.refresh();
    oldActive.refresh();

    assertThat(oldDraft.getPrograms()).hasSize(2);
    assertThat(oldDraft.getLifecycleStage()).isEqualTo(LifecycleStage.ACTIVE);
    assertThat(oldActive.getLifecycleStage()).isEqualTo(LifecycleStage.OBSOLETE);

    this.versionRepository.setLive(oldActive.id);

    oldActive.refresh();
    oldDraft.refresh();
    assertThat(oldActive.getLifecycleStage()).isEqualTo(LifecycleStage.ACTIVE);
    assertThat(oldDraft.getLifecycleStage()).isEqualTo(LifecycleStage.OBSOLETE);

    this.versionRepository.setLive(oldDraft.id);

    oldActive.refresh();
    oldDraft.refresh();
    assertThat(oldActive.getLifecycleStage()).isEqualTo(LifecycleStage.OBSOLETE);
    assertThat(oldDraft.getLifecycleStage()).isEqualTo(LifecycleStage.ACTIVE);
  }

  @Test
  public void testTransactionality() {
    Transaction outer = DB.getDefault().beginTransaction();
    assertThat(outer.isActive()).isTrue();
    Version draft = versionRepository.getDraftVersion();
    assertThat(outer.isActive()).isTrue();
    Version draft2 = versionRepository.getDraftVersion();
    assertThat(outer.isActive()).isTrue();
    outer.rollback();
    assertThat(outer.isActive()).isFalse();
    assertThat(draft).isEqualTo(draft2);
  }
}
