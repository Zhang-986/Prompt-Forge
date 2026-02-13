package test

type ListNode struct {
	Val  int
	Next *ListNode
}

func getIntersectionNode(headA, headB *ListNode) *ListNode {
	// 指针走完，然后同时从两个不同的出发点出发
	slow, fast := headA, headB
	for slow != fast {
		if slow == nil {
			slow = headB
		} else {
			slow = slow.Next
		}
		if fast == nil {
			fast = headA
		} else {
			fast = fast.Next
		}
	}
	return slow
}
