/*
 * Copyright (c) 2025, cha-ndler
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.collectionloghelper.data;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * A composable tree node describing when a guidance step should be considered complete.
 *
 * <p>This is the tagged union that powers B1 ("Composable AND/OR completion conditions").
 * Java 11 does not support {@code sealed} interfaces, so the closed hierarchy is expressed
 * via an abstract class with a finite set of package-local subtypes:
 *
 * <ul>
 *   <li>{@link Leaf} — wraps a single {@link CompletionCondition} atomic check.</li>
 *   <li>{@link All} — true when every child node evaluates true.</li>
 *   <li>{@link Any} — true when any child node evaluates true.</li>
 * </ul>
 *
 * <p>The existing single-enum {@code completionCondition} field on {@link GuidanceStep}
 * remains the canonical legacy shape. When a step defines a {@link CompletionNode}, the
 * sequencer walks the tree for eager (polled) completion checks; leaf evaluation matches
 * the semantics of the legacy switch exactly, so a step written as the legacy single enum
 * or as a {@code Leaf} of that enum behaves identically.
 *
 * <p>Instances are immutable. {@link All} and {@link Any} defensively copy and wrap their
 * children with {@link Collections#unmodifiableList(List)} on construction.
 */
public abstract class CompletionNode
{
	/** Node kind tag — useful for logging, equality, and discriminated serialization. */
	public enum Kind
	{
		LEAF,
		ALL,
		ANY
	}

	CompletionNode()
	{
		// Package-private constructor: only the nested subtypes may extend.
	}

	public abstract Kind getKind();

	/**
	 * Walks the node tree, evaluating every leaf via the supplied predicate.
	 *
	 * @param leafEvaluator maps a {@link CompletionCondition} to a boolean pass/fail
	 * @return {@code true} when the composed tree evaluates to complete
	 */
	public abstract boolean evaluate(Predicate<CompletionCondition> leafEvaluator);

	public static Leaf leaf(CompletionCondition condition)
	{
		return new Leaf(condition);
	}

	public static All all(List<CompletionNode> children)
	{
		return new All(children);
	}

	public static Any any(List<CompletionNode> children)
	{
		return new Any(children);
	}

	/** Wraps a single legacy {@link CompletionCondition} as the atomic leaf of the tree. */
	public static final class Leaf extends CompletionNode
	{
		private final CompletionCondition condition;

		Leaf(CompletionCondition condition)
		{
			if (condition == null)
			{
				throw new IllegalArgumentException("Leaf condition must not be null");
			}
			this.condition = condition;
		}

		public CompletionCondition getCondition()
		{
			return condition;
		}

		@Override
		public Kind getKind()
		{
			return Kind.LEAF;
		}

		@Override
		public boolean evaluate(Predicate<CompletionCondition> leafEvaluator)
		{
			return leafEvaluator.test(condition);
		}

		@Override
		public boolean equals(Object obj)
		{
			return obj instanceof Leaf && ((Leaf) obj).condition == this.condition;
		}

		@Override
		public int hashCode()
		{
			return condition.hashCode();
		}

		@Override
		public String toString()
		{
			return "Leaf(" + condition + ")";
		}
	}

	/** True when every child evaluates true. Empty {@code All} is vacuously true. */
	public static final class All extends CompletionNode
	{
		private final List<CompletionNode> children;

		All(List<CompletionNode> children)
		{
			this.children = freeze(children);
		}

		public List<CompletionNode> getChildren()
		{
			return children;
		}

		@Override
		public Kind getKind()
		{
			return Kind.ALL;
		}

		@Override
		public boolean evaluate(Predicate<CompletionCondition> leafEvaluator)
		{
			for (CompletionNode child : children)
			{
				if (!child.evaluate(leafEvaluator))
				{
					return false;
				}
			}
			return true;
		}

		@Override
		public boolean equals(Object obj)
		{
			return obj instanceof All && ((All) obj).children.equals(this.children);
		}

		@Override
		public int hashCode()
		{
			return 31 + children.hashCode();
		}

		@Override
		public String toString()
		{
			return "All" + children;
		}
	}

	/** True when any child evaluates true. Empty {@code Any} is vacuously false. */
	public static final class Any extends CompletionNode
	{
		private final List<CompletionNode> children;

		Any(List<CompletionNode> children)
		{
			this.children = freeze(children);
		}

		public List<CompletionNode> getChildren()
		{
			return children;
		}

		@Override
		public Kind getKind()
		{
			return Kind.ANY;
		}

		@Override
		public boolean evaluate(Predicate<CompletionCondition> leafEvaluator)
		{
			for (CompletionNode child : children)
			{
				if (child.evaluate(leafEvaluator))
				{
					return true;
				}
			}
			return false;
		}

		@Override
		public boolean equals(Object obj)
		{
			return obj instanceof Any && ((Any) obj).children.equals(this.children);
		}

		@Override
		public int hashCode()
		{
			return 37 + children.hashCode();
		}

		@Override
		public String toString()
		{
			return "Any" + children;
		}
	}

	private static List<CompletionNode> freeze(List<CompletionNode> children)
	{
		if (children == null || children.isEmpty())
		{
			return Collections.emptyList();
		}
		for (CompletionNode child : children)
		{
			if (child == null)
			{
				throw new IllegalArgumentException("CompletionNode children must not contain null entries");
			}
		}
		return Collections.unmodifiableList(new java.util.ArrayList<>(children));
	}
}
