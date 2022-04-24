import java.util.Collections;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.LocalDate;
import java.time.Instant;
import java.time.ZoneId;
import java.time.Clock;

public class Solution {
    public static void main(String[] args) {
        System.out.println("Random Insertion Stability Testing:");
        final int TEST_ATTEMPTS = 10;
        for (int i = 0; i != TEST_ATTEMPTS; i++) {
            String prefix = "[" + (i + 1) + "/" + TEST_ATTEMPTS + "] ";
            String output1 = runSetup().toString(), output2 = runSetup().toString();
            System.out.println(prefix + (output1.equals(output2) ? "Outputs match!" : "Outputs don't match!!"));
        }
        System.out.println("\nSample output:\n");
        System.out.println(runSetup());
    }
    
    private static TodoList runSetup() {
        // Create tasks
        ITodoTask taskFixLights = new TodoTask("Fix lights", createDate("2022-05-22"));
        ITodoTask taskAttendSeminar = new TodoTask("Attend seminar", createDate("2022-05-10"));
        ITodoTask taskPrepareReport = new TodoTask("Prepare iteration 1 reports", createDate("2022-04-10"));
        ITodoTask taskSubmitHW = new TodoTask("Submit design patterns HW", createDate("2022-04-26"));
        ITodoTask taskAddressFeedback = new TimeTrackingDecorator(
            new TodoTask("Address TA/Instructor feedback", createDate("2022-05-02"))
        );
        ITodoTask taskDefineClasses = new StatusHistoryDecorator(
            new TodoTask("Define classes", createDate("2022-04-20"))
        );
        ITodoTask taskDesignBackend = new TodoTask("Design backend APIs", createDate("2022-04-30"));
        ITodoTask taskImplementFrontend = new StatusHistoryDecorator(
            new TimeTrackingDecorator(
                new TodoTask("Implement front-end components", createDate("2022-05-01"))
            )
        );
        ITodoTask taskApples = new TodoTask("Apples", createDate("2022-04-27"));
        ITodoTask taskBananas = new TodoTask("Bananas", createDate("2022-04-25"));
        ITodoTask taskOranges = new TodoTask("Oranges", createDate("2022-04-22"));
        ITodoTask taskMilk = new TodoTask("Milk", createDate("2022-04-29"));
        ITodoTask taskYoghurt = new TodoTask("Yoghurt", createDate("2022-04-23"));

        // Change the state of some tasks
        taskFixLights.progressTask();
        taskPrepareReport.completeTask();
        taskSubmitHW.progressTask();
        taskDefineClasses.progressTask();
        taskDefineClasses.completeTask();
        taskDesignBackend.progressTask();
        taskImplementFrontend.progressTask();
        taskBananas.completeTask();
        taskOranges.completeTask();
        taskMilk.completeTask();

        // Create lists
        TodoList mainList = new TodoList("My Todos");
        TodoList cs319List = new TodoList("CS 319", new TargetDateSortingStrategy());
        TodoList implementationList = new TodoList("Implementation", new TargetDateSortingStrategy());
        TodoList groceryList = new TodoList("Grocery");
        TodoList groceryFruitsList = new TodoList("Fruits", new AlphabeticalSortingStrategy());
        TodoList groceryDairyList = new TodoList("Dairy");

        // Randomly execute list insertions
        List<Function> insertions = new ArrayList<>();
        insertions.addAll(Arrays.asList(new Function[] {
            () -> cs319List.insert(taskAddressFeedback),
            () -> cs319List.insert(taskPrepareReport),
            () -> cs319List.insert(taskSubmitHW),
            () -> implementationList.insert(taskDefineClasses),
            () -> implementationList.insert(taskDesignBackend),
            () -> implementationList.insert(taskImplementFrontend),
            () -> cs319List.insert(implementationList),
            () -> groceryFruitsList.insert(taskApples),
            () -> groceryFruitsList.insert(taskBananas),
            () -> groceryFruitsList.insert(taskOranges),
        }));
        Collections.shuffle(insertions);
        for (Function function : insertions)
            function.call();

        // Insertions with "Add Order" sorting need to be non-random
        mainList.insert(taskFixLights);
        mainList.insert(taskAttendSeminar);
        groceryDairyList.insert(taskMilk);
        groceryDairyList.insert(taskYoghurt);
        groceryList.insert(groceryFruitsList);
        groceryList.insert(groceryDairyList);
        mainList.insert(cs319List);
        mainList.insert(groceryList);
        
        return mainList;
    }

    private static Instant createDate(String parseableDate) {
        return LocalDate.parse(parseableDate).atStartOfDay(ZoneId.systemDefault()).toInstant();
    }

    private interface Function {
        void call();
    }
}

interface ITodoComponent {
    String getDescription();
}

class TodoList implements ITodoComponent {
    private String description;
    private List<ITodoComponent> contents;
    private ITodoTaskSortingStrategy sortingStrategy;

    public TodoList(String description) {
        this(description, new AddOrderSortingStrategy());
    }

    public TodoList(String description, ITodoTaskSortingStrategy sortingStrategy) {
        this.description = description;
        this.contents = new ArrayList<>();
        this.sortingStrategy = sortingStrategy;
    }

    public void insert(ITodoComponent item) {
        sortingStrategy.insert(contents, item);
    }

    @Override
    public String toString() {
        String result = description + " " + sortingStrategy + " {";

        for (ITodoComponent todoItem : contents) {
            result += "\n";
            if (todoItem instanceof ITodoTask)
                result += "\t- " + todoItem;
            else if (todoItem instanceof TodoList) {
                String[] lines = todoItem.toString().split("\\R");
                for (int i = 0; i != lines.length; i++) {
                    result += "\t" + lines[i];
                    if (i + 1 != lines.length)
                        result += "\n";
                }
            }
        }

        result += "\n}";
        return result;
    }

    @Override
    public String getDescription() {
        return description;
    }
}

interface ITodoTask extends ITodoComponent {
    String getDescription();
    ITodoTaskState getState();
    Instant getTargetDate();
    void progressTask();
    void completeTask();
}

class TodoTask implements ITodoTask {
    private String description;
    private ITodoTaskState state;
    private Instant targetDate;

    public TodoTask(String description, Instant targetDate) {
        this.description = description;
        this.targetDate = targetDate;
        this.state = new CreatedState();
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public ITodoTaskState getState() {
        return state;
    }

    @Override
    public Instant getTargetDate() {
        return targetDate;
    }

    @Override
    public void progressTask() {
        state = state.progressTask();
    }

    @Override
    public void completeTask() {
        state = state.completeTask();
    }

    @Override
    public String toString() {
        return description + " " + DateTimeFormatter.ofPattern("uuuu-MM-dd").withZone(ZoneId.systemDefault()).format(targetDate) + " " + state;
    }
}

abstract class BaseTodoTaskDecorator implements ITodoTask {
    protected ITodoTask task;

    public BaseTodoTaskDecorator(ITodoTask task) {
        this.task = task;
    }
}

class TimeTrackingDecorator extends BaseTodoTaskDecorator {
    private Instant startDate;

    public TimeTrackingDecorator(ITodoTask task) {
        super(task);
        this.startDate = Clock.systemUTC().instant();
    }

    @Override
    public String toString() {
        return task.toString() + " [Elapsed Time: " + getElapsedTime() + " day(s)]";
    }

    public long getElapsedTime() {
        return ChronoUnit.DAYS.between(Clock.systemUTC().instant(), startDate);
    }

    @Override
    public String getDescription() {
        return task.getDescription();
    }

    @Override
    public ITodoTaskState getState() {
        return task.getState();
    }

    @Override
    public Instant getTargetDate() {
        return task.getTargetDate();
    }

    @Override
    public void progressTask() {
        task.progressTask();
    }

    @Override
    public void completeTask() {
        task.completeTask();
    }
}

class StatusHistoryDecorator extends BaseTodoTaskDecorator {
    private List<ITodoTaskState> stateHistory;

    public StatusHistoryDecorator(ITodoTask task) {
        super(task);
        stateHistory = new ArrayList<>();
        stateHistory.add(task.getState());
    }

    @Override
    public void progressTask() {
        task.progressTask();
        stateHistory.add(task.getState());
    }

    @Override
    public void completeTask() {
        task.completeTask();
        stateHistory.add(task.getState());
    }

    @Override
    public String toString() {
        String result = task.toString() + " [Status History: ";

        for (int i = 0; i != stateHistory.size(); i++) {
            result += stateHistory.get(i);
            if (i + 1 != stateHistory.size())
                result += "->";
        }

        result += "]";
        return result;
    }

    @Override
    public String getDescription() {
        return task.getDescription();
    }

    @Override
    public ITodoTaskState getState() {
        return task.getState();
    }

    @Override
    public Instant getTargetDate() {
        return task.getTargetDate();
    }
}

interface ITodoTaskState {
    ITodoTaskState progressTask();
    ITodoTaskState completeTask();
}

class CreatedState implements ITodoTaskState {
    @Override
    public String toString() {
        return "[Created]";
    }

    @Override
    public ITodoTaskState progressTask() {
        return new InProgressState();
    }

    @Override
    public ITodoTaskState completeTask() {
        return new CompletedState();
    }
}

class InProgressState implements ITodoTaskState {
    @Override
    public String toString() {
        return "[In Progress]";
    }

    @Override
    public ITodoTaskState progressTask() {
        return this;
    }

    @Override
    public ITodoTaskState completeTask() {
        return new CompletedState();
    }
}

class CompletedState implements ITodoTaskState {
    @Override
    public String toString() {
        return "[Completed]";
    }

    @Override
    public ITodoTaskState progressTask() {
        return this;
    }

    @Override
    public ITodoTaskState completeTask() {
        return this;
    }
}

interface ITodoTaskSortingStrategy {
    void sort(List<ITodoComponent> contents);
    void insert(List<ITodoComponent> contents, ITodoComponent item);
}

abstract class BaseTodoTaskSortingStrategy implements ITodoTaskSortingStrategy {
    private void sort(List<ITodoComponent> contents, Comparator<? super ITodoComponent> compare) {
        contents.sort((o1, o2) -> {
            if (o1 instanceof TodoList && o2 instanceof TodoList) return 0;
            if (o1 instanceof TodoList || o2 instanceof TodoList) return 1;
            return compare(o1, o2);
        });
    }

    private void insert(List<ITodoComponent> contents, ITodoComponent item, Comparator<? super ITodoComponent> compare) {
        if (item instanceof TodoList) {
            contents.add(item);
            return;
        }

        for (int i = 0; i != contents.size(); i++) {
            if (contents.get(i) instanceof TodoList || compare(contents.get(i), item) > 0) {
                contents.add(i, item);
                return;
            }
        }
        contents.add(item);
    }

    @Override
    public void sort(List<ITodoComponent> contents) {
        sort(contents, (o1, o2) -> compare(o1, o2));
    }

    @Override
    public void insert(List<ITodoComponent> contents, ITodoComponent item) {
        insert(contents, item, (o1, o2) -> compare(o1, o2));
    }

    abstract protected int compare(ITodoComponent o1, ITodoComponent o2);
}

class AlphabeticalSortingStrategy extends BaseTodoTaskSortingStrategy {
    @Override
    protected int compare(ITodoComponent comp1, ITodoComponent comp2) {
        return comp1.getDescription().compareToIgnoreCase(comp2.getDescription());
    }

    @Override
    public String toString() {
        return "[Alphabetical Order]";
    }
}

class AddOrderSortingStrategy extends BaseTodoTaskSortingStrategy {
    @Override
    protected int compare(ITodoComponent o1, ITodoComponent o2) {
        return 0;
    }

    @Override
    public String toString() {
        return "[Add Order]";
    }
}

class TargetDateSortingStrategy extends BaseTodoTaskSortingStrategy {
    @Override
    protected int compare(ITodoComponent o1, ITodoComponent o2) {
        return ((ITodoTask) o1).getTargetDate().compareTo(((ITodoTask) o2).getTargetDate());
    }

    @Override
    public String toString() {
        return "[Target Date Order]";
    }
}
