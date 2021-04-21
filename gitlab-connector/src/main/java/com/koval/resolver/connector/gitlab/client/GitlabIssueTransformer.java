package com.koval.resolver.connector.gitlab.client;

import com.koval.resolver.common.api.bean.issue.*;
import com.koval.resolver.common.api.component.connector.IssueTransformer;
import com.koval.resolver.connector.gitlab.exception.GitlabClientException;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Assignee;
import org.gitlab4j.api.models.Author;
import org.gitlab4j.api.models.Discussion;
import org.gitlab4j.api.models.Note;
import org.joda.time.DateTime;

import java.net.URI;
import java.util.*;

public class GitlabIssueTransformer implements IssueTransformer<org.gitlab4j.api.models.Issue> {

  private static final Map<String, User> USER_CACHE = new HashMap<>();
  private static final String UNKNOWN = "<unknown>";
  private static final Map<Integer, Project> PROJECT_CACHE = new HashMap<>();

  private final GitLabApi apiClient;

  public GitlabIssueTransformer(final GitLabApi apiClient) {
    this.apiClient = apiClient;
  }

  @Override
  public Issue transform(final org.gitlab4j.api.models.Issue originalIssue) {
    final Issue transformedIssue = new Issue();

    transformedIssue.setLink(URI.create(originalIssue.getWebUrl()));
    transformedIssue.setKey(originalIssue.getId().toString());
    transformedIssue.setSummary(originalIssue.getTitle());
    transformedIssue.setDescription(originalIssue.getDescription());
    transformedIssue.setStatus(originalIssue.getTaskStatus());
    transformedIssue.setResolution("");
    transformedIssue.setPriority("");

    if (originalIssue.getAuthor() == null) {
      transformedIssue.setReporter(getUnknownUser());
    } else {
      transformedIssue.setReporter(transformAuthor(originalIssue.getAuthor()));
    }
    if (originalIssue.getAssignee() == null) {
      transformedIssue.setAssignee(getUnknownUser());
    } else {
      transformedIssue.setAssignee(transformAssignee(originalIssue.getAssignee()));
    }
    if (originalIssue.getMilestone() != null) {
      transformedIssue.setIssueType(new IssueType(originalIssue.getMilestone().getTitle(), false));
    } else {
      transformedIssue.setIssueType(null);
    }
    transformedIssue.setProject(transformProject(originalIssue.getProjectId()));
    transformedIssue.setCreationDate(new DateTime(originalIssue.getClosedAt()));
    transformedIssue.setUpdateDate(new DateTime(originalIssue.getUpdatedAt()));
    transformedIssue.setDueDate(new DateTime(originalIssue.getDueDate()));

    transformedIssue.setLabels(originalIssue.getLabels());
    transformedIssue.setComponents(transformComponents(originalIssue));
    transformedIssue.setFixVersions(transformFixVersions(originalIssue));
    transformedIssue.setAffectedVersions(transformAffectedVersions(originalIssue));
    transformedIssue.setComments(transformComments(originalIssue));
    transformedIssue.setIssueLinks(transformIssueLinks(originalIssue));
    transformedIssue.setAttachments(transformAttachments(originalIssue));
    transformedIssue.setSubTasks(transformSubTasks(originalIssue));
    transformedIssue.setIssueFields(new ArrayList<>());

    return transformedIssue;
  }

  @Override
  public List<Issue> transform(final Collection<org.gitlab4j.api.models.Issue> originalIssues) {
    final List<Issue> transformedIssues = new ArrayList<>();
    for (final org.gitlab4j.api.models.Issue originalIssue : originalIssues) {
      transformedIssues.add(transform(originalIssue));
    }
    return transformedIssues;
  }

  private Project transformProject(final Integer projectId) {
    if (projectId == null) {
      return getUnknownProject();
    }
    if (PROJECT_CACHE.containsKey(projectId)) {
      return PROJECT_CACHE.get(projectId);
    }
    org.gitlab4j.api.models.Project project = null;
    try {
      project = apiClient.getProjectApi().getProject(projectId);
    } catch (GitLabApiException e) {
      throw new GitlabClientException("Could not get project by id = " + projectId, e);
    }
    final Project transformedProject = new Project(
            String.valueOf(project.getId()),
            project.getName()
    );
    PROJECT_CACHE.put(projectId, transformedProject);
    return transformedProject;
  }

  private List<Component> transformComponents(final org.gitlab4j.api.models.Issue originalIssue) {
    final List<Component> transformedComponents = new ArrayList<>();
    return transformedComponents;
  }

  private List<Version> transformFixVersions(final org.gitlab4j.api.models.Issue originalIssue) {
    final List<Version> transformedFixVersions = new ArrayList<>();
    return transformedFixVersions;
  }

  private List<Version> transformAffectedVersions(final org.gitlab4j.api.models.Issue originalIssue) {
    final List<Version> transformedAffectedVersions = new ArrayList<>();
    return transformedAffectedVersions;
  }

  private List<Comment> transformComments(final org.gitlab4j.api.models.Issue originalIssue) {
    final List<Comment> transformedComments = new ArrayList<>();
    List<Discussion> discussions = null;
    try {
      discussions = apiClient.getDiscussionsApi().getIssueDiscussions(originalIssue.getProjectId(), originalIssue.getIid());
    } catch (GitLabApiException e) {
      e.printStackTrace();
      throw new GitlabClientException("Could not get components by issueId = " + originalIssue.getIid(), e);
    }
    if (discussions != null) {
      for (Discussion discussion : discussions) {
        for (Note originalComment : discussion.getNotes()) {
          final Comment transformedComment = new Comment(transformAuthor(originalComment.getAuthor()),
                  getUnknownUser(),
                  new DateTime(originalComment.getCreatedAt()),
                  new DateTime(originalComment.getUpdatedAt()),
                  originalComment.getBody());
          transformedComments.add(transformedComment);
        }
      }
    }
    return transformedComments;
  }

  private List<IssueLink> transformIssueLinks(final org.gitlab4j.api.models.Issue originalIssue) {
    final List<IssueLink> transformedIssueLinks = new ArrayList<>();
    return transformedIssueLinks;
  }

  private List<Attachment> transformAttachments(final org.gitlab4j.api.models.Issue originalIssue) {
    final List<Attachment> transformedAttachments = new ArrayList<>();
    return transformedAttachments;
  }

  private List<SubTask> transformSubTasks(final org.gitlab4j.api.models.Issue originalIssue) {
    final List<SubTask> transformedSubTasks = new ArrayList<>();
    return transformedSubTasks;
  }

  private User transformAuthor(final Author originalUser) {
    if (originalUser == null) {
      return getUnknownUser();
    }
    if (USER_CACHE.containsKey(originalUser.getUsername())) {
      return USER_CACHE.get(originalUser.getUsername());
    }
    final User transformedUser = new User(
            originalUser.getUsername(),
            originalUser.getName(),
            originalUser.getEmail(),
            new ArrayList<>(),
            URI.create(originalUser.getAvatarUrl()),
            URI.create("")
    );
    USER_CACHE.put(originalUser.getUsername(), transformedUser);
    return transformedUser;
  }

  private User transformAssignee(final Assignee originalUser) {
    if (originalUser == null) {
      return getUnknownUser();
    }
    if (USER_CACHE.containsKey(originalUser.getUsername())) {
      return USER_CACHE.get(originalUser.getUsername());
    }
    final User transformedUser = new User(
            originalUser.getUsername(),
            originalUser.getName(),
            originalUser.getEmail(),
            new ArrayList<>(),
            URI.create(originalUser.getAvatarUrl()),
            URI.create("")
    );
    USER_CACHE.put(originalUser.getUsername(), transformedUser);
    return transformedUser;
  }

  private User getUnknownUser() {
    return new User(UNKNOWN, UNKNOWN, UNKNOWN, new ArrayList<>(), URI.create(""), URI.create(""));
  }

  private Project getUnknownProject() {
    return new Project(UNKNOWN, UNKNOWN);
  }
}
