import { and, desc, eq } from "drizzle-orm";
import { getDb } from "../../../db";
import { merchants, products } from "../../../db/schema";
import { slugify, toRouteErrorMessage } from "../../../lib/api-helpers";
import { requireAdminApiUser } from "../../../lib/admin-auth";

const MISSING_TABLE_HINT =
  "The products table is unavailable. Generate the migration locally with `npm run db:generate`, then deploy so the platform can apply the generated SQL to the real D1 database.";

export async function GET(request: Request) {
  try {
    const url = new URL(request.url);
    const merchantIdParam = url.searchParams.get("merchantId");
    const category = url.searchParams.get("category");

    const conditions = [];
    if (merchantIdParam) {
      const merchantId = Number(merchantIdParam);
      if (!Number.isInteger(merchantId)) {
        return Response.json({ error: "invalid merchantId" }, { status: 400 });
      }
      conditions.push(eq(products.merchantId, merchantId));
    }
    if (category) {
      conditions.push(eq(products.category, category));
    }

    const db = getDb();
    const rows = await db
      .select({
        id: products.id,
        merchantId: products.merchantId,
        merchantName: merchants.name,
        name: products.name,
        slug: products.slug,
        description: products.description,
        category: products.category,
        priceCents: products.priceCents,
        imageUrl: products.imageUrl,
        isFlashDeal: products.isFlashDeal,
        isMostGhosted: products.isMostGhosted,
        isActive: products.isActive,
        createdAt: products.createdAt,
        updatedAt: products.updatedAt,
      })
      .from(products)
      .leftJoin(merchants, eq(products.merchantId, merchants.id))
      .where(conditions.length ? and(...conditions) : undefined)
      .orderBy(desc(products.createdAt), desc(products.id));

    return Response.json({ products: rows });
  } catch (error) {
    return Response.json(
      { error: toRouteErrorMessage(error, MISSING_TABLE_HINT) },
      { status: 500 }
    );
  }
}

export async function POST(request: Request) {
  const unauthorized = await requireAdminApiUser();
  if (unauthorized) return unauthorized;

  try {
    const payload = (await request.json()) as {
      merchantId?: number;
      name?: string;
      category?: string;
      description?: string;
      priceCents?: number;
      imageUrl?: string;
      isFlashDeal?: boolean;
      isMostGhosted?: boolean;
      isActive?: boolean;
      slug?: string;
    };

    const name = payload.name?.trim() ?? "";
    const category = payload.category?.trim() ?? "";
    const merchantId = payload.merchantId;

    if (!name) {
      return Response.json({ error: "name is required" }, { status: 400 });
    }
    if (!category) {
      return Response.json({ error: "category is required" }, { status: 400 });
    }
    if (!Number.isInteger(merchantId)) {
      return Response.json({ error: "merchantId is required" }, { status: 400 });
    }
    if (
      payload.priceCents !== undefined &&
      (!Number.isInteger(payload.priceCents) || payload.priceCents < 0)
    ) {
      return Response.json(
        { error: "priceCents must be a non-negative integer" },
        { status: 400 }
      );
    }

    const slug = slugify(payload.slug?.trim() || name);
    if (!slug) {
      return Response.json(
        { error: "could not derive a slug from name" },
        { status: 400 }
      );
    }

    const db = getDb();
    const [product] = await db
      .insert(products)
      .values({
        merchantId: merchantId as number,
        name,
        slug,
        category,
        description: payload.description?.trim() ?? "",
        priceCents: payload.priceCents ?? 0,
        imageUrl: payload.imageUrl?.trim() || null,
        isFlashDeal: payload.isFlashDeal ?? false,
        isMostGhosted: payload.isMostGhosted ?? false,
        isActive: payload.isActive ?? true,
      })
      .returning();

    return Response.json({ product }, { status: 201 });
  } catch (error) {
    const message = toRouteErrorMessage(error, MISSING_TABLE_HINT);
    const isDuplicate = message.includes("UNIQUE constraint failed");
    const isBadMerchant = message.includes("FOREIGN KEY constraint failed");
    const status = isDuplicate ? 409 : isBadMerchant ? 400 : 500;
    const friendly = isDuplicate
      ? "a product with this slug already exists"
      : isBadMerchant
        ? "merchantId does not reference an existing merchant"
        : message;

    return Response.json({ error: friendly }, { status });
  }
}
