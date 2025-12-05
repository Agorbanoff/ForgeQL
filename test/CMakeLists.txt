cmake_minimum_required(VERSION 4.0)
project(DSA C)

set(CMAKE_C_STANDARD 11)

# Vector executable
add_executable(Vector_Demo
        DataStructures/Vector/vector.c
        DataStructures/Vector/vector.h
        DataStructures/Vector/main.c)

# Linked List executable
add_executable(LinkedList_Demo
        DataStructures/Linked_List/Linked_list.c
        DataStructures/Linked_List/Linked_list.h
        DataStructures/Linked_List/main.c)


add_executable(Stack_Demo
        DataStructures/Stack/Stack.c
        DataStructures/Stack/Stack.h
        DataStructures/Stack/Main.c)

add_executable(Queue_Demo
        DataStructures/Queue/Queue.c
        DataStructures/Queue/Queue.h
        DataStructures/Queue/Main.c)

add_executable(Insertion_Demo
        Algorithms/Insertion_sort.c)

add_executable(Selection_sort
        Algorithms/Selection_sort.c)

add_executable(Binary_search
        Algorithms/Binary_search.c)

add_executable(Bubble_sort
        Algorithms/Bubble_sort.c)

add_executable(Merge_sort
        Algorithms/Merge_sort.c)