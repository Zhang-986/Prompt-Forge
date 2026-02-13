package test

type ListNode struct {
	Val  int
	Next *ListNode
}

func getIntersectionNode(headA, headB *ListNode) *ListNode {
	// 快慢指针
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
