package io.crnk.core.engine.internal.repository;

import io.crnk.core.boot.CrnkBoot;
import io.crnk.core.engine.internal.utils.MultivaluedMap;
import io.crnk.core.engine.registry.RegistryEntry;
import io.crnk.core.engine.registry.ResourceRegistry;
import io.crnk.core.engine.url.ConstantServiceUrlProvider;
import io.crnk.core.mock.MockConstants;
import io.crnk.core.mock.models.Project;
import io.crnk.core.mock.models.RelationIdTestResource;
import io.crnk.core.mock.models.Schedule;
import io.crnk.core.mock.models.Task;
import io.crnk.core.mock.repository.MockRepositoryUtil;
import io.crnk.core.mock.repository.ProjectRepository;
import io.crnk.core.mock.repository.RelationIdTestRepository;
import io.crnk.core.mock.repository.ScheduleRepositoryImpl;
import io.crnk.core.mock.repository.TaskRepository;
import io.crnk.core.module.discovery.ReflectionsServiceDiscovery;
import io.crnk.core.queryspec.QuerySpec;
import io.crnk.core.repository.implicit.ImplicitOwnerBasedRelationshipRepository;
import io.crnk.core.resource.registry.ResourceRegistryTest;
import java.util.Arrays;
import java.util.List;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ImplicitOwnerBasedRelationshipRepositoryTest {


	private ImplicitOwnerBasedRelationshipRepository relRepository;

	private ScheduleRepositoryImpl scheduleRepository;

	private Schedule schedule3;

	private RelationIdTestRepository testRepository;

	private RelationIdTestResource resource;

	private Schedule schedule4;

	private ProjectRepository projectRepository;

	private Project project;

	private TaskRepository taskRepository;

	private Task task;

	private ImplicitOwnerBasedRelationshipRepository taskProjectRepository;

	@Before
	public void setup() {
		MockRepositoryUtil.clear();

		CrnkBoot boot = new CrnkBoot();
		boot.setServiceDiscovery(new ReflectionsServiceDiscovery(MockConstants.TEST_MODELS_PACKAGE));
		boot.setServiceUrlProvider(new ConstantServiceUrlProvider(ResourceRegistryTest.TEST_MODELS_URL));
		boot.boot();

		ResourceRegistry resourceRegistry = boot.getResourceRegistry();

		RegistryEntry entry = resourceRegistry.getEntry(RelationIdTestResource.class);
		relRepository =
				(ImplicitOwnerBasedRelationshipRepository) entry.getRelationshipRepositoryForType("schedules", null)
						.getRelationshipRepository();

		taskProjectRepository = new ImplicitOwnerBasedRelationshipRepository(Task.class, Project.class);
		taskProjectRepository.setResourceRegistry(resourceRegistry);

		testRepository = (RelationIdTestRepository) entry.getResourceRepository().getResourceRepository();
		testRepository.setResourceRegistry(resourceRegistry);
		resource = new RelationIdTestResource();
		resource.setId(2L);
		resource.setName("relationId");
		testRepository.create(resource);


		scheduleRepository = new ScheduleRepositoryImpl();
		schedule3 = new Schedule();
		schedule3.setId(3L);
		schedule3.setName("schedule");
		scheduleRepository.create(schedule3);

		schedule4 = new Schedule();
		schedule4.setId(4L);
		schedule4.setName("schedule");
		scheduleRepository.create(schedule4);

		projectRepository = new ProjectRepository();
		project = new Project();
		project.setId(42L);
		project.setName("project");
		projectRepository.save(project);

		taskRepository = new TaskRepository();
		task = new Task();
		task.setId(13L);
		task.setName("task");
		taskRepository.save(task);
	}


	@Test
	public void checkSetRelationId() {
		relRepository.setRelation(resource, 3L, "testSerializeEager");
		Assert.assertEquals(3L, resource.getTestSerializeEagerId().longValue());
		Assert.assertNull(resource.getTestSerializeEager());

		Assert.assertSame(schedule3,
				relRepository.findOneTarget(resource.getId(), "testSerializeEager", new QuerySpec(Schedule.class)));

		MultivaluedMap targets =
				relRepository.findTargets(Arrays.asList(resource.getId()), "testSerializeEager", new QuerySpec(Schedule.class));
		Assert.assertEquals(1, targets.keySet().size());
		Object target = targets.getUnique(resource.getId());
		Assert.assertEquals(schedule3, target);

		relRepository.setRelation(resource, null, "testSerializeEager");
		Assert.assertNull(resource.getTestSerializeEagerId());
		Assert.assertNull(resource.getTestSerializeEager());
	}

	@Test
	public void checkSetRelationIds() {
		relRepository.setRelations(resource, Arrays.asList(3L, 4L), "testMultipleValues");
		Assert.assertEquals(Arrays.asList(3L, 4L), resource.getTestMultipleValueIds());

		List targets =
				relRepository.findManyTargets(resource.getId(), "testMultipleValues", new QuerySpec(Schedule.class));
		Assert.assertEquals(2, targets.size());
		Assert.assertSame(schedule3, targets.get(0));
		Assert.assertSame(schedule4, targets.get(1));

		MultivaluedMap targetsMap =
				relRepository.findTargets(Arrays.asList(resource.getId()), "testMultipleValues", new QuerySpec(Schedule.class));
		Assert.assertEquals(1, targetsMap.keySet().size());
		targets = targetsMap.getList(resource.getId());
		Assert.assertEquals(2, targets.size());
		Assert.assertSame(schedule3, targets.get(0));
		Assert.assertSame(schedule4, targets.get(1));
	}

	@Test
	public void checkAddRemoveRelationIds() {
		relRepository.addRelations(resource, Arrays.asList(3L, 4L), "testMultipleValues");
		Assert.assertEquals(Arrays.asList(3L, 4L), resource.getTestMultipleValueIds());

		relRepository.addRelations(resource, Arrays.asList(5L), "testMultipleValues");
		Assert.assertEquals(Arrays.asList(3L, 4L, 5L), resource.getTestMultipleValueIds());

		relRepository.removeRelations(resource, Arrays.asList(3L), "testMultipleValues");
		Assert.assertEquals(Arrays.asList(4L, 5L), resource.getTestMultipleValueIds());
	}

	@Test
	public void checkSetRelation() {
		taskProjectRepository.setRelation(task, 42L, "project");
		Assert.assertEquals(42L, task.getProject().getId().longValue());

		Assert.assertSame(project,
				taskProjectRepository.findOneTarget(task.getId(), "project", new QuerySpec(Task.class)));
	}

	@Test
	public void checkSetRelations() {
		taskProjectRepository.setRelations(task, Arrays.asList(42L), "projects");
		Assert.assertEquals(1, task.getProjects().size());
		Assert.assertEquals(42L, task.getProjects().iterator().next().getId().longValue());

		MultivaluedMap targets =
				taskProjectRepository.findTargets(Arrays.asList(task.getId()), "projects", new QuerySpec(Task.class));
		Assert.assertEquals(1, targets.keySet().size());
		Assert.assertEquals(13L, targets.keySet().iterator().next());
		Assert.assertEquals(project, targets.getUnique(13L));
	}


	@After
	public void teardown() {
		MockRepositoryUtil.clear();
	}

}
