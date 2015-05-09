class ProductsController < ApplicationController
  def matching_color
      render json: Sphereio.product_with_matching_color(params['color'])
  end
end